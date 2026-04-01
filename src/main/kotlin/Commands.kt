package org.example

import org.example.telegram.command

val blankCommand = command("`", "default")
val deleteMsgCommand = command("Закрыть", "deleteMsg", 1)

val joinCommand = command("", "join", 2)
val updateCommand = command("Обновить список игр", "update", 1)

val playerNumCommand = command("", "playerNum", 3)
val playerConfirmCommand = command("Ввести ▶️", "playerConfirm", 3)
val confirmNumCommand = command("Подтвердить", "confirmNum", 2)
val mainMenuCommand = command("🔙 Покинуть игру", "mainMenu", 1)

val detailsCommand = command("", "details", 2)
val renameCommand = command("✍️ Переименовать", "rename", 2)
val positionCommand = command("Указать номер", "posi", 3)
val handCommand = command("✋Поднять руку", "hand", 1)
val kickCommand = command("❌ Исключить", "kick", 2)

val resetNumsCommand = command("🚮 Сбросить номера игроков", "resetNums", 1)
val confirmResetCommand = command("Да", "confirmReset", 2)

val unkickCommand = command("Впустить", "unkick", 2)

val hostBackCommand = command("Назад", "back", 1)
val menuKickCommand = command("🚪 Список исключенных игроков", "menuKick", 2)

val changeHostCommand = command("🤝 Сменить ведущего", "changeHost", 1)
val newHostCommand = command("", "newHost", 2)
val stopRehostingCommand = command("◀️ Отмена", "stopRehosting", 1)

val acceptHostingCommand = command("Да", "acceptHosting", 3)
val declineHostingCommand = command("Нет", "declineHosting", 3)

val menuLobbyCommand = command("◀️ Меню игроков", "menuLobby", 1)
val menuRolesCommand = command("Меню ролей ▶️", "menuRoles", 1)
val menuPreviewCommand = command("Меню распределения ▶️", "menuPreview", 1)

val posSetCommand = command("Ввести ▶️", "posSet", 3)

val nameCancelCommand = command("Отмена", "nameCancel", 1)

val dummyCommand = command("➕ Добавить игрока", "dummy", 1)
val roleCommand = command("", "role", 2)
val incrCommand = command("➕", "incr", 2)
val decrCommand = command("➖", "decr", 2)

val changeScriptCommand = command("🎭 Сменить набор ролей", "changeScript", 2)
val resetRolesCommand = command("🚮 Сбросить выбор ролей", "resetRoles", 2)
val previewCommand = command("🔀 Раздать роли", "preview", 2)

val menuWeightCommand = command("⚖️ Статистика игроков", "menuWeight", 1)
val toggleHideRolesCommand = command("🕶️ Скрывать роли", "toggleHideRoles", 1)

val menuDistributionCommand = command("", "menuDistribution", 1)

val reassignRoleCommand = command("◀️ Выбранные роли", "reassignRoles", 2)
val reassignAnyCommand = command("Все роли ▶️", "reassignAny", 2)
val deletePairCommand = command("😶‍🌫️ Убрать роль", "deletePair", 1)
val reassignConfirmCommand = command("", "reassignConfirm", 2)
val swapPairsCommand = command("↔️ Поменять роли местами", "swapPairs", 1)
val swapConfirmCommand = command("", "swapConfirm", 2)

val gameCommand = command("Начать игру 🎮", "game", 2)
val gameModeCommand = command("", "mode", 2)

val markBotCommand = command("🌚", "markBot", 2)
val proceedCommand = command("☀️ Начать день", "proceed", 1)

val dayDetailsCommand = command("", "dayDetails", 2)
val statusCommand = command("Статус: Ошибка", "status", 2)
val killCommand = command("💀", "kill", 2)
val reviveCommand = command("🏩", "rviv", 2)
val fallCommand = command("", "fall", 2)

val dayBackCommand = command("◀️ Назад", "dayBack", 1)

val settingsCommand = command("⚙️ Опции", "settings", 1)
val settingsBackCommand = command("◀️ Назад", "settingsBack", 2)

val settingDescCommand = command("-", "settingDesc", 3)
val hostSettingCommand = command("-", "hostSetting", 3)
val autoSingLimDescCommand = command("👤🌙 Лимит времени игрока", "autoSingLimDesc", 2)
val autoSingLimSelCommand = command("", "autoSingLimSel", 2)
val autoTeamLimDescCommand = command("👥🌙 Лимит времени команды", "autoTeamLimDesc", 2)
val autoTeamLimSelCommand = command("", "autoTeamLimSel", 2)

val shareGameCommand = command("🔓 Поделиться игрой", "shareGame", 1)
val shareSelectCommand = command("", "shareSelect", 2)

val timerCommand = command("⏳ Таймер", "timer")
val nightCommand = command("🌙 Начать ночь", "night", 1)

val selectCommand = command("", "select", 3)
val executeActionCommand = command("Подтвердить ▶️", "executeAction", 2)
val nextRoleCommand = command("Следующая роль ▶️", "nextRole", 1)
val skipRoleCommand = command("Пропустить ⏩", "skipRole", 1)
val cancelActionCommand = command("◀️ Отменить действие", "cancelAction", 1)
val dayCommand = command("☀️ Начать день", "day", 1)

val autoNightCommand = command("🤖🌙 Автоночь", "autoNight", 1)
val autoNightUpdCommand = command("🔄 Обновить статус", "autoNightUpd", 1)

val autoNightPlayCommand = command("👀 Проснуться", "autoNightPlay", 3)
val selectTargetCommand = command("", "selectTarget", 3)
val autoNightSkipCommand = command("💤 Пропустить", "autoNightSkip", 2)
val autoNightDoneCommand = command("✅ Подтвердить", "autoNightDone", 2)

val forceLeadCommand = command("⏫ Стать лидером", "forceLead", 2)
val leadConfirmCommand = command("Да", "leadConfirm", 3)

val hidePlayersCommand = command("🕶️ Скрыть игроков", "hidePlayers", 1)
val filterCommand = command("Фильтр: Ошибка", "fltr", 1)

val timerDeleteCommand = command("❌️", "timerDelete", 1)
val timerStateCommand = command("", "timerState", 1)
val timerResetCommand = command("🔄", "timerReset", 1)

val selectWinnerCommand = command("", "selectWinner", 2)

val revealRoleCommand = command("👀 Показать роль", "reveal", 2)
val gameInfoCommand = command("ℹ️ Информация об игре", "gameInfo", 2)
val aliveInfoCommand = command("👥 Живые игроки", "liveInfo", 3)
val playerMenuCommand = command("", "playerMenu", 3)

val updateCheckCommand = command("", "updateCheck", 2)

val hostRequestCommand = command("📩 Запросы на ведение", "hostRequests", 2)
val hostSettingsCommand = command("😎 Список ведущих", "hostSettings", 2)
val adminSettingsCommand = command("⚛️ Список администраторов", "adminSettings", 2)
val gamesSettingsCommand = command("🎮 Список игр", "gamesSettings", 3)
val hostAdminSettingsCommand = command("⚙️ Список настроек ведущих", "hostAdminSettings", 2)
val advertCommand = command("📺 Реклама", "advert", 0)

val timeLimitOnCommand = command("Off", "timeLimitOn", 2)
val timeLimitOffCommand = command("❌", "timeLimitOff", 2)
val gameLimitOnCommand = command("Off", "gameLimitOn", 2)
val gameLimitOffCommand = command("❌", "gameLimitOff", 2)
val shareCommand = command("Off", "share", 2)
val canReassignCommand = command("Off", "canReassign", 2)
val distributionCommand = command("Off", "distribution", 2)
val deleteHostCommand = command("❌ Удалить ведущего", "deleteHost", 2)
val promoteHostCommand = command("🧑‍🧒‍🧒 Сделать администратором", "promoteHost", 2)
val allowHostCommand = command("✅", "allowHost", 2)
val denyHostCommand = command("❌", "denyHost", 2)
val removeAdminCommand = command("❌", "removeAdmin", 3)
val chooseHostAdminCommand = command("`", "chooseHostAdmin", 2)
val chooseHostSettingsCommand = command("", "chooseHostSettings", 2)
val changeHostAdminSettingCommand = command("`", "changeHostAdminSetting", 3)
val adminBackCommand = command("Назад", "adminBack", 1)

val confirmPromoteCommand = command("Да", "confirmPromote", 3)

val terminateGameCommand = command("❌ Остановить игру", "terminateGame", 2)
val confirmTerminateCommand = command("Да", "confirmTerminate", 3)

val sendAdCommand = command("", "sendAd", 2)
val sendAdHistoryCommand = command("", "sendAdHistory", 2)
val adSelectCommand = command("Выбрать", "adSelect", 2)
val adClearCommand = command("Закрыть", "adClear", 1)

val acceptNameCommand = command("Да", "nameAccept", 3)
val cancelName = command("Нет", "nameDeny", 2)

val acceptStopCommand = command("Да", "stopAccept", 2)
val acceptLeaveCommand = command("Да", "leaveAccept", 2)
val acceptRehostCommand = command("Да", "rehostAccept", 1)
val acceptEndCommand = command("👑 Подтвердить", "endAccept", 2)

val stopLobbyCommand = command("🚪 В главное меню", "stopLobby", 1)

val closePopupCommand = command("Закрыть", "closePopup", 1)

val scriptStatCommand = command("", "scriptStatCommand", 2)

val adCommand = command("/ad")
val adNewCommand = command("/newad")
val adminCommand = command("/admin")

val hostCommand = command("/host")
val rehostCommand = command("/rehost")
val startCommand = command("/start")
val menuCommand = command("/menu")
val changeNameCommand = command("/changename")

val startGameCommand = command("🎮 Запустить игру")
val restartGameCommand = command("🔙 В лобби")
val endGameCommand = command("👑 Завершить игру")
val stopGameCommand = command("🚪 В главное меню")
val leaveGameCommand = command("🚪 Покинуть игру")

val statCommand = command("📚 Статистика")
val adminPanelCommand = command("⚛️ Меню администратора")

val startGameLegacyCommand = command("Запустить игру")
val restartGameLegacyCommand = command("Перезапустить игру")
val stopGameLegacyCommand = command("Завершить игру")
val leaveGameLegacyCommand = command("Покинуть игру")

val adminPanelLegacyCommand = command("Меню администратора")