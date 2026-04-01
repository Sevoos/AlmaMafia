package org.example

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bson.types.ObjectId
import org.example.game.*
import org.example.game.stopGame
import org.example.lua.*
import org.example.telegram.*
import org.slf4j.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass

// todo keep 'leave game' button under game message, remove footer
// todo add ability to rehost game while it is in progress
// todo improve naming in hosts alive player count message
// todo add win rate stat for player
// todo rework town, make it savable
// todo make lazy, but updating on data change getters for loading related object from db by their id (generate getters with annotations?)
// todo replace objectid and get rid of mongo dependencies
// todo optimize db by using better serialization formats and maybe create some mongoshell-like util for hand processing data (kotlin-script?)
// todo add capabilities for graceful shutdown for bot (make sure to delete temp folder before finishing)
// todo refactor EVERYTHING

const val gameDurationLimitHours = 6
const val gameHistoryTtlHours = 24
const val sendPendingAfterSec = 3
const val deleteNumUpdateMsgAfterSec = 30
const val timerTtlMin = 60L
const val timerMaxTimeMin = 5L
const val timerInactiveTimeMin = 2L
const val roleNameLen = 32
const val roleDescLen = 280
const val defaultPageSize: Int = 1//0
const val statusScriptName = "[status]"
val deleteForceLeadConfirmAfter: Duration = Duration.ofSeconds(10)
val numbers = arrayOf("0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣")
val notKnowingTeams = arrayOf("none", "city")
val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
val inGameStates = setOf(GameState.TYPE, GameState.REVEAL, GameState.GAME)

val resetAccount: Account.() -> Unit = {
    state = AccountState.Menu
    menuMessageId = -1L
    connectionId = null
}
val tempDir: Path = Files.createTempDirectory("scripts")
val log: Logger = logger<Main>()
val towns = mutableMapOf<GameId, Town>()
val scripts: MutableMap<GameId, Map<String, Script>> = mutableMapOf()

class Main

fun main() {
    //val connectionString = "mongodb://EdgeDom:WontH4CKAGA1n@localhost:44660/?retryWrites=true&w=majority"
    tempDir.toFile().deleteOnExit()

    migrate {
        migration("move_default_scripts") {
            val config = Config()
            val gameScript = GameScript(
                GameScriptId(),
                config.author,
                "Friday Night Mafia",
                "./template.json"
            )
            gameScripts.save(gameScript)
            val from = Paths.get(config.path, "scripts")
            val to = from.resolve(config.author.toString())
                .resolve(gameScript.id.toString())
            if (!Files.exists(to)) {
                Files.createDirectories(to)
            }
            File("./config.json").writeText(
                Json { prettyPrint = true }.encodeToString(
                    Config.serializer(),
                    config.copy(defaultScriptId = gameScript.id.toString()),
                )
            )
            Files.newDirectoryStream(from).use { stream ->
                for (entry in stream) {
                    if (Files.isRegularFile(entry)) {
                        val target = to.resolve(entry.fileName)
                        Files.copy(entry, target)
                    }
                }
            }
        }
        migration("make_default_script_weighted") {
            gameScripts.save(defaultGameScript()!!.copy(roleDistribution = RoleDistribution.WEIGHTED))
        }
        /*migration("delete_broken_stats") {
            teamHistories.find().forEach {
                if (it.team == "null") {
                    teamHistories.delete(it.id)
                }
            }
        }*/
    }

    val bot = bot {
        token = Config().botToken
        dispatch {
            text {
                handle(message.text?.trim() ?: "") by MafiaHandler.textHandler
            }

            callbackQuery {
                handle(callbackQuery.data) by MafiaHandler.queryHandler
            }
        }
    }

    runBlocking {
        launch {
            bot.startPolling()
        }

        launch {
            while (true) {
                try {
                    val now = Date()
                    val filter: TimedMessage.() -> Boolean = { date.before(now) }
                    timedMessages.find(filter).forEach {
                        bot.deleteMessage(ChatId.fromId(it.chatId), it.messageId)
                    }
                    timedMessages.deleteMany(filter)
                } catch (e: Exception) {
                    log.error("Unable to process bomb messages", e)
                }
                delay(10000)
            }
        }

        launch {
            while (true) {
                try {
                    hostInfos.deleteMany {
                        (timeLimit && until.before(Date())) || (gameLimit && left < 1)
                    }
                } catch (e: Exception) {
                    log.error("Unable to process host info expires", e)
                }
                delay(60000)
            }
        }

        launch {
            while (true) {
                try {
                    val set = mutableSetOf<Pending>()
                    pendings.find {
                        date.toInstant().isBefore(Instant.now())
                    }.forEach {
                        set.add(it)
                    }
                    pendings.deleteMany { this in set }
                    set.forEach {
                        accounts.get(it.host)?.let {
                            games.find { hostId == it.chatId }.singleOrNull()?.let { game ->
                                if (game.state == GameState.CONNECT) {
                                    showLobbyMenu(it.chatId, it.menuMessageId, game, bot)
                                } else if (game.state == GameState.REVEAL) {
                                    showRevealMenu(game, bot, it.chatId, it.menuMessageId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("Unable to process pending", e)
                }
                delay(1000)
            }
        }

        launch {
            while (true) {
                try {
                    timers.find { active }.forEach {
                        val now = System.currentTimeMillis()

                        val update = now - it.timestamp
                        timers.update(it.chatId) {
                            time += update
                            timestamp = now
                            if (timerText(it.time) != timerText(it.time + update)) {
                                updated = true
                            }
                        }
                    }
                    timers.find {
                        val ttl = Duration.ofMillis(System.currentTimeMillis() - creationTime)
                            .minus(Duration.ofMinutes(timerTtlMin)).isPositive
                        val timeLimit = Duration.ofMillis(time).minus(Duration.ofMinutes(timerMaxTimeMin)).isPositive
                        val inactiveTime = Duration.ofMillis(System.currentTimeMillis() - timestamp)
                            .minus(Duration.ofMinutes(timerInactiveTimeMin)).isPositive
                        ttl || timeLimit || inactiveTime
                    }.forEach {
                        deleteTimer(it, bot)
                    }
                    timers.find { updated }.forEach {
                        updateTimer(it, bot)
                    }
                } catch (e: Exception) {
                    log.error("Unable to process timers", e)
                }
                delay(1000)
            }
        }

        launch {
            while (true) {
                try {
                    games.find {
                        val date = playedAt ?: createdAt
                        date.toInstant().isBefore(Instant.now().minusSeconds(gameDurationLimitHours * 60 * 60L))
                    }.forEach {
                        stopGame(
                            it,
                            it.hostId,
                            bot,
                            accounts.get(it.hostId)?.menuMessageId ?: -1L
                        )
                    }
                } catch (e: Exception) {
                    log.error("Unable to process game TTLs", e)
                }
                delay(60000)
            }
        }

        launch {
            while (true) {
                try {
                    gameHistory.deleteMany {
                        playedAt.toInstant().isBefore(Date().toInstant().minusSeconds(gameHistoryTtlHours * 60 * 60L))
                    }
                } catch (e: Exception) {
                    log.error("Unable to process game history TTLs", e)
                }
                delay(60000)
            }
        }

        launch {
            while (true) {
                try {
                    val set = mutableSetOf<NightMessageUpdate>()
                    nightMessageUpdates.find().forEach { set.add(it) }
                    if (set.isNotEmpty()) {
                        nightMessageUpdates.deleteMany { this in set }
                        set.forEach { update ->
                            update.action?.let { action ->
                                action.actorLinks.forEach { link ->
                                    link.actor?.connection?.nightPlayerMessage?.let { msg ->
                                        towns[action.gameId]?.let { town ->
                                            showAutoNightPlayerMenu(
                                                town.night[action.wakeId],
                                                town,
                                                link,
                                                msg.chatId,
                                                msg.messageId,
                                                bot
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    log.error("Unable to process auto-night updates", e)
                }
                delay(5000)
            }
        }
    }
}

fun Bot.error(chatId: Long, text: String = "Неизвестная команда.") {
    sendMsg(
        chatId,
        text
    ).inlineKeyboard { button(deleteMsgCommand, it) }
}

fun defaultGameScript() = gameScripts.get(GameScriptId(Config().defaultScriptId))

private fun isKnownHost(chatId: Long) = hostInfos.get(chatId) != null

fun showAd(game: Game, connections: List<Connection>, bot: Bot, messageId: Long, chatId: Long) {
    val id = ObjectId()
    val chat = ChatId.fromId(chatId)
    bot.editMessageText(
        chat,
        messageId,
        text = "Возможные сообщения:"
    )
    val adList = ads.find()
    val messages = adList.map { message ->
        bot.sendMsg(
            chatId,
            message.text
        ).inlineKeyboard {
            button(adSelectCommand, message.id, id)
        }.updateContext(bot, chatId)
    }
    messages.last().updateKeyboard {
        inlineKeyboard {
            button(adSelectCommand, adList.last().id, id)
            button(adClearCommand, id)
        }
    }
    adTargets.save(AdTarget(id, game, connections, messages.mapNotNull { it.msgId } + listOf(messageId)))
}



fun selectAd(bot: Bot, game: Game, ad: Message, connections: List<Connection>) {
    val host = game.hostId
    fun send(chatId: Long) {
        bot.sendMsg(
            chatId,
            ad.text
        ).then { msgId ->
            timedMessages.save(
                TimedMessage(
                    ObjectId(),
                    chatId,
                    msgId,
                    Date(System.currentTimeMillis() + 1000 * 60 * 60)
                )
            )
        }
    }
    send(host)
    connections.forEach {
        send(it.playerId)
    }
}

// todo move
internal fun updateSettingsView(
    hostId: Long,
    messageId: Long,
    gameMessageId: Long,
    game: Game,
    town: Town?,
    bot: Bot,
    update: HostSettings.() -> Unit
) {
    hostSettings.update(hostId, update)
    hostSettings.get(hostId)?.let { settings ->
        if (gameMessageId != -1L && town != null) {
            showDayMenu(town, hostId, gameMessageId, bot, game)
        }
        showSettingsMenu(settings, hostId, messageId, gameMessageId, bot)
        return
    }
}

fun showPlayerDayDesc(town: Town, playerPos: Int, messageId: Long, chatId: Long, bot: Bot) {
    town.playerMap[playerPos]?.let<Person, Unit> { player ->
        val fallMode = games.get(town.gameId)?.host?.settings?.fallMode ?: false
        bot.editMessageReplyMarkup(
            ChatId.fromId(chatId),
            messageId,
            replyMarkup = inlineKeyboard {
                button(blankCommand named "Детали")
                button(
                    dayDetailsCommand named
                            player.desc(
                                roles = !isHideRolesMode(games.get(town.gameId))
                            ), playerPos, messageId
                )
                row {
                    playerDayDesc(player, messageId, fallMode)
                }
                button(dayBackCommand, messageId)
            }
        )
        return@let
    }
}

fun isHideRolesMode(game: Game?): Boolean {
    return game?.let { game ->
        game.host?.settings?.hideRolesMode
    } ?: false
}

fun deleteTimer(
    it: Timer,
    bot: Bot
) {
    timers.delete(it.chatId)
    bot.deleteMessage(ChatId.fromId(it.chatId), it.messageId)
}

private fun updateTimer(
    timer: Timer,
    bot: Bot
) {
    val text = timerText(timer.time)
    bot.editMessageReplyMarkup(
        ChatId.fromId(timer.chatId),
        timer.messageId,
        replyMarkup = inlineKeyboard {
            button(blankCommand named text)
            row {
                button(timerResetCommand, timer.chatId)
                button(
                    timerStateCommand named (if (timer.active) "⏸️" else "▶️"),
                    timer.chatId
                )
                button(timerDeleteCommand, timer.chatId)
            }
        }
    )
    timers.update(timer.chatId) { updated = false }
}

private fun timerText(time: Long): String {
    val timePassed = time / 1000
    val min = (timePassed / 60).toString().padStart(2, '0').map { numbers[it.toString().toInt()] }.joinToString("")
    val sec = (timePassed % 60).toString().padStart(2, '0').map { numbers[it.toString().toInt()] }.joinToString("")
    val text = "$min：$sec"
    return text
}

fun showHostSettings(
    bot: Bot,
    chatId: Long,
    messageId: Long,
    itemsOffset: Int
) {
    showPaginatedMenu(
        chatId,
        messageId,
        replyMarkup = inlineKeyboard {
            button(blankCommand named "Список ведущих")
            hostInfos.find().forEach {
                accounts.get(it.chatId)?.let { acc ->
                    row {
                        button(blankCommand named ("👤 " + acc.fullName()))
                    }
                    row {
                        button(blankCommand named "🎮 Лимит игр")
                        if (it.gameLimit) {
                            button(gameLimitOnCommand named it.left.toString(), it.chatId, messageId)
                            button(gameLimitOffCommand, it.chatId, messageId)
                        } else {
                            button(gameLimitOnCommand, it.chatId, messageId)
                        }
                    }
                    row {
                        button(blankCommand named "⏰ Срок ведения")
                        if (it.timeLimit) {
                            button(timeLimitOnCommand named it.until.toString(), it.chatId, messageId)
                            button(timeLimitOffCommand, it.chatId, messageId)
                        } else {
                            button(timeLimitOnCommand, it.chatId, messageId)
                        }
                    }
                    row {
                        button(blankCommand named "👥 Передавать ведение")
                        button(shareCommand named if (it.canShare) "✅" else "❌", it.chatId, messageId)
                    }
                    row {
                        button(blankCommand named "👇 Выбирать роли")
                        button(canReassignCommand named if (it.canReassign) "✅" else "❌", it.chatId, messageId)
                    }
                    row {
                        button(blankCommand named "⚖️ Распределения игроков")
                        button(distributionCommand named if (it.showDistribution) "✅" else "❌", it.chatId, messageId)
                    }
                    if (admins.get(it.chatId) == null) {
                        button(promoteHostCommand, it.chatId, messageId)
                    } else {
                        button(blankCommand named "⚛️ Администратор")
                    }
                    button(deleteHostCommand, it.chatId, messageId)
                }
            }
        },
        adminBackCommand,
        hostSettingsCommand,
        itemsOffset
    )
}

fun showHostRequests(
    bot: Bot,
    chatId: Long,
    messageId: Long,
    itemsOffset: Int = 0
) {
    showPaginatedMenu(
        chatId,
        messageId,
        bot,
        "Запросы на ведение",
        hostRequests.find(),
        { index, hostRequest ->
            accounts.get(hostRequest.chatId)?.let { acc ->
                button(blankCommand named "${index + 1}. ${acc.fullName()}")
                row {
                    button(allowHostCommand, hostRequest.chatId, messageId)
                    button(denyHostCommand, hostRequest.chatId, messageId)
                }
            }
        },
        adminBackCommand,
        hostRequestCommand,
        itemsOffset
    )
}

fun updateCheck(
    param: String
) {
    val check = checks.get(param)
    if (check == null) {
        checks.save(Check(ObjectId(), param, true))
    } else {
        checks.update(param) {
            state = !check.state
        }
    }
}

fun canHost(
    chatId: Long
): Boolean {
    return if (checks.get(CheckOption.HOST_KNOWN)) {
        hostInfos.find { this.chatId == chatId }.isNotEmpty()
    } else {
        true
    }
}

internal fun leaveGame(
    chatId: Long,
    messageId: Long,
    resetAccount: Account.() -> Unit,
    bot: Bot
) {
    accounts.update(chatId, resetAccount)

    val cons = connections.find { playerId == chatId }
    connections.deleteMany { playerId == chatId }
    bot.deleteMessage(
        ChatId.fromId(chatId),
        messageId
    )
    val conIds = cons.map { it.id }.toSet()
    val chat = ChatId.fromId(chatId)
    nightPlayerMessages.find { playerId in conIds }.forEach {
        bot.deleteMessage(
            chat,
            it.messageId
        )
    }
    nightPlayerMessages.deleteMany { playerId in conIds }
    messageLinks.find(chatId) { chatId == it }.forEach {
        bot.deleteMessage(
            chat,
            it.messageId
        )
        messageLinks.delete(it.id)
    }
    showMainMenu(chatId, "Возвращаемся в главное меню.", bot, true)

    cons.forEach { con ->
        games.get(con.gameId)?.hostId?.let {
            pendings.save(Pending(ObjectId(), it, con.gameId))
        }
    }
}

fun updateSetup(game: Game) {
    try {
        val script = game.script!!
        val json = File(
            "${script.path}/${script.jsonPath}"
        ).readText()
        val data = Json.decodeFromString<GameSet>(json)
        roles.deleteMany { gameId == game.id }
        data.roles.forEachIndexed { index, it ->
            val role = Role(
                ObjectId(),
                game.id,
                it.displayName,
                it.desc,
                it.defaultTeam,
                it.defaultType,
                it.name,
                it.priority,
                it.coverName
            )
            role.index = index
            roles.save(
                role
            )
        }
        types.deleteMany { gameId == game.id }
        data.type.forEachIndexed { index, it ->
            types.save(
                Type(
                    ObjectId(),
                    game.id,
                    it.name,
                    it.displayName,
                    it.choice,
                    it.passive,
                    index
                )
            )
        }
        data.teamDisplayNames.forEach {
            teamNames.save(TeamName(ObjectId(), game.id, it.key, it.value))
        }
    } catch (e: Exception) {
        log.error("Unable to update setups for game: $game", e)
    }
}

fun shortLog(town: Town, showRoles: Boolean = true): String {
    return if (town.actions.isNotEmpty()) {
        val set = mutableSetOf<Pair<KClass<out Event>, Int>>()
        val text =
            town.actions.asSequence().filter { it.skippedBy == null }.map { it.events() }.flatten().sortedBy { it.pos }
                .map {
                    val pair = it::class to it.pos
                    if (pair !in set) {
                        set.add(pair)
                        town.playerMap[it.pos]?.let { player ->
                            "${it.symbol()} Игрок ${player.desc(" - ", false, showRoles)} ${it.desc()}"
                        }
                    } else {
                        null
                    }
                }.filterNotNull().joinToString("\n")
        text
    } else {
        ""
    }
}

fun fullLog(town: Town, hideRoles: Boolean): String {
    return if (town.actions.isNotEmpty()) {
        val text = town.actions.filter {
            (it.skippedBy == null || !it.skippedBy!!.master)
                    && it.dependencies.all {
                it.skippedBy == null || !it.skippedBy!!.master
            }
        }.mapIndexed { i, it ->
            val action = it.desc()

            val alive = it.actors.filter { it.alive }
            val who = if (alive.isNotEmpty()) {
                alive.joinToString(", ") { it.desc() }
            } else {
                "Действующее лицо не указно"
            }
            val target = it.selection.joinToString { it.desc(" - ") }
            val skipper = it.skippedBy?.let {
                if (it.master)
                    "Ведущий"
                else
                    it.actors
                        .filter { it.alive }
                        .maxByOrNull { it.roleData.priority }
                        ?.roleData?.displayName
                        ?: "Действующее лицо не указано"
            }
            val dep = it.dependencies.lastOrNull()?.let {
                it.desc() + "(" +
                        it.actors
                            .filter { it.alive }
                            .maxByOrNull { it.roleData.priority }
                            ?.roleData?.displayName +
                        ")"
            }
            "Событие ${i + 1}." + (if (it.skippedBy != null) " (ОТМЕНЕНО)" else "") + "\n" +
                    "Кто: $who\n" +
                    "Действие: $action\n" +
                    "Цель: $target\n" +
                    (if (it is InfoAction) "Результат: ${it.text}\n" else "") +
                    (if (dep != null) "Реакция на: $dep\n" else "") +
                    (if (skipper != null) "Отменено ролью: $skipper\n" else "")
        }.joinToString("\n\n")
        if (hideRoles) "<span class=\"tg-spoiler\">$text</span>" else text
    } else {
        ""
    }
}

//private fun desc(player: Person, sep: String = ". ") = "${player.pos}$sep${player.name} (${player.role.name})"

fun initAccount(
    userName: String,
    chatId: Long,
    bot: Bot
) {
    if (accounts.get(chatId) == null) {
        accounts.save(Account(ObjectId(), chatId, userName))
    } else {
        accounts.update(chatId) {
            state = AccountState.Init
            menuMessageId = -1L
            connectionId = null
        }
    }
    bot.sendMsg(
        chatId,
        "Пожалуйста, введите свое имя. Это имя смогут видеть ведущие игр, к которым вы присоединяетесь.",
        ReplyKeyboardRemove(true)
    )
}

fun showMainMenu(
    chatId: Long,
    menuText: String,
    bot: Bot,
    forceUpdate: Boolean = false,
    silent: Boolean = false
) {
    bot.sendMessage(
        ChatId.fromId(chatId),
        menuText,
        disableNotification = silent,
        replyMarkup = mafiaKeyboard(chatId)
    )
    showGames(chatId, -1L, bot, forceUpdate)
}

fun withAccount(chatId: Long, func: (Account) -> Unit) {
    accounts.get(chatId)?.let {
        func(it)
    }
}

fun menuButtons(
    messageId: Long
): InlineKeyboardMarkup {
    return inlineKeyboard {
        games.find { actual }.forEach {
            accounts.get(it.hostId)?.let { host ->
                row {
                    button(
                        joinCommand named (if (it.state in inGameStates) "🎮" else "👥") + host.fullName(),
                        it.id,
                        messageId
                    )
                }
            }
        }
        row { button(updateCommand, messageId) }
    }
}

fun lobby(messageId: Long, game: Game): InlineKeyboardMarkup {
    val players = connections.find { gameId == game.id }
    return inlineKeyboard {
        val playerList = players.sortedWith(compareBy({ it.pos }, { it.createdAt }))
        doubleColumnView(playerList).default {
            button(blankCommand)
            button(blankCommand)
        }.build {
            button(detailsCommand named it.name(), it.id, messageId)
            button(
                if (it.pos == Int.MAX_VALUE || it.pos < 1)
                    positionCommand
                else positionCommand named it.pos.toString(),
                it.id,
                0,
                messageId
            )
        }
        button(blankCommand named "Игроков: ${players.size}")

        button(dummyCommand, messageId)
        if (game.kickList.isNotEmpty()) {
            button(menuKickCommand, messageId)
        }
        //row { button(resetNumsCommand, messageId) }
        if (game.creator?.hostInfo?.canShare == true) {
            button(changeHostCommand, messageId)
        }
        button(menuRolesCommand, messageId)
    }
}

fun showGames(
    chatId: Long,
    messageId: Long,
    bot: Bot,
    forceUpdate: Boolean = false
) {
    val msgId = if (messageId == -1L || forceUpdate) {
        bot.sendMsg(chatId, "Доступные игры (нажмите на игру чтобы присоединиться):").msgId
    } else {
        messageId
    }
    if (msgId != null) {
        accounts.update(chatId) { menuMessageId = msgId }
        bot.editMessageReplyMarkup(
            ChatId.fromId(chatId),
            msgId,
            replyMarkup = menuButtons(msgId)
        )
    }
}

fun mafiaKeyboard(chatId: Long, definition: FooterContext.() -> Unit = {}) = footerKeyboard {
    accounts.get(chatId)?.let {
        when (it.state) {
            AccountState.Menu -> {
                if (isKnownHost(chatId)) {
                    button(startGameCommand)
                }
            }

            AccountState.Host -> {
                if (games.find { hostId == it.chatId }.any { it.id in towns }) {
                    button(endGameCommand)
                    button(restartGameCommand)
                }
                button(stopGameCommand)
            }

            AccountState.Lobby -> {
                button(leaveGameCommand)
            }

            else -> {}
        }
    }
    definition()

    row {
        if (isAdmin(chatId)) {
            button(adminPanelCommand)
        }
        if (checks.get(CheckOption.SHOW_STATS)) {
            button(statCommand)
        }
    }
}