# InvChest

[简体中文](#简体中文) | [English (US)](#english-us)

> **AI 生成声明**：本项目包含 AI 生成内容，代码仅供正常使用、参考和学习，**请不要将其再次用于 AI 训练**！
> **AI Generated Disclaimer**: This project contains AI-generated content. The code is provided for normal use, reference, and learning only. **Please do not use it for AI training again!**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.9-informational.svg)](https://github.com/EndlessMCOrg/InvChest/releases)

---

## 简体中文

### 简介

InvChest 是一个面向 Bukkit/Spigot/Paper/Purpur/Folia 服务器的插件。玩家可以绑定容器，插件会按配置的策略定时将容器中的物品自动传输到玩家背包。插件支持多语言与可配置的传输策略，并为后续拓展 Sponge、(Neo)Forge、Fabric 预留架构。

### 功能特性

- **容器绑定**：通过命令绑定或解绑容器，并支持管理员为其他玩家管理绑定。
- **自动传输**：按配置的间隔与策略，将容器中的物品自动传输到玩家背包。
- **容器保护**：阻止其他玩家打开或破坏已绑定容器，并限制外部提取。
- **多语言**：内置简体中文、繁体中文（台湾/香港）与美式英语。
- **可扩展架构**：模块化设计，为 Sponge、(Neo)Forge、Fabric 适配层预留接口。

### 环境要求

- 服务端核心：Bukkit、Spigot、Paper、Purpur、Folia。
- Minecraft 版本：1.12+。
- Java：8 或更高版本（目标字节码为 Java 8）。
- 构建工具：Maven（仅构建时需要）。

### 安装

- 将打包生成的 `InvChest.jar` 放入服务器的 `plugins/` 目录。
- 首次启动后会生成 `config.yml` 与 `lang/` 目录（内置 `zh_cn`、`zh_tw`、`zh_hk`、`en_us` 四种语言）。
- 如需修改语言，在 `config.yml` 中设置 `lang`。

### 配置（`config.yml`）

- `transfer-interval`：传输间隔（秒），默认 `30`。
- `transfer-mode`：`all` 或 `try`。
  - `all`：尽量将容器物品移入玩家背包；背包放不下的部分掉落在玩家位置；达到单次上限后剩余物品保留到下次。
  - `try`：按顺序尽量移入；遇到无法容纳的物品时停止；容器保留剩余物品。
- `max-items-per-transfer`：单次传输的物品数量上限（按堆叠数量累加，`all` 与 `try` 模式均生效）。
- `lang`：`zh_cn`、`zh_tw`、`zh_hk`、`en_us`，或 `auto_en_us`（控制台固定指定语言，玩家根据客户端语言自动匹配）。
- `bind.max-per-player`：每位玩家最多可绑定的容器数量（`0` 为不限制）。
- `bind.default-chunk-range`：默认区块常加载范围（`-1` 不常加载，`0` 仅所在区块，`>0` 半径）。
- `bind.auto-name-format`：自动命名格式，`{number}` 会替换为序号。
- `chunk-loading.enabled`：是否启用区块常加载。
- `chunk-loading.max-range`：允许的最大区块加载范围。
- `ui.list-items-per-page`：列表命令每页显示条数。
- `transfer-message-enabled`：是否在发生传输时发送提示。

### 命令

- `/invchest bind [名称] [区块范围]` 或 `/ic bind ...`：绑定玩家当前所看的容器。
- `/invchest unbind [名称]` 或 `/ic unbind ...`：解绑；不填名称时解绑当前所看的容器。
- `/invchest list [页码]` 或 `/ic list [页码]`：查看自己的绑定列表。
- `/invchest lock <名称> <true|false>` 或 `/ic lock ...`：设置其他玩家能否打开该容器。
- `/invchest start|stop [名称]` 或 `/ic start|stop ...`：开始/暂停自己的传输；不填名称时作用于全部。
- 管理员命令：
  - `/invchest abind <玩家> [名称] [区块范围]` 或 `/ic abind ...`：为指定玩家绑定（管理员需面向目标容器）。
  - `/invchest aunbind <玩家> [名称]` 或 `/ic aunbind ...`：为指定玩家解绑。
  - `/invchest alist [玩家|@all] [页码]` 或 `/ic alist ...`：查看指定玩家或全服绑定。
  - `/invchest astart|astop <玩家|@all> [名称]` 或 `/ic astart|astop ...`：开始/暂停指定玩家或全服传输。
  - `/invchest reload` 或 `/ic reload`：重载配置。

### 权限

- `invchest.bind`：绑定/解绑/查看自己的容器（默认开启）。
- `invchest.lock`：修改自己容器的访问锁定状态（默认开启）。
- `invchest.statue`：开始/暂停自己的传输（默认开启）。
- `invchest.abind`：管理员为他人绑定/解绑/查看（默认 OP）。
- `invchest.astatue`：管理员为他人或全局开始/暂停传输（默认 OP）。
- `invchest.inf`：在保护提示中显示容器所有者（默认 OP）。
- `invchest.reload`：重载配置（默认 OP）。

### 容器与保护

- 支持容器：箱子、陷阱箱、桶、潜影盒（双箱/双陷阱箱会作为整体处理；不支持铜质容器）。
- 绑定后：
  - 其他玩家打开该容器会被阻止。
  - 破坏受保护容器仅允许绑定者或具有 `invchest.abind` 的玩家。
  - 允许外部输入（如漏斗、投掷器），禁止外部输出（如漏斗、漏斗矿车）。
  - 容器被破坏或不存在时自动解绑。

### 多语言

- 资源目录：`src/main/resources/lang/`。
- 运行时目录：`plugins/InvChest/lang/`。
- 内置语言：`zh_cn.yml`、`zh_tw.yml`、`zh_hk.yml`、`en_us.yml`。
- 新增语言：将 `<code>.yml` 放入资源目录后重新构建即可。
- 自动模式：`lang: auto_<code>` 时，控制台按 `<code>` 输出，玩家依据客户端语言匹配；缺失的键会回退到 `en_us`。

### 构建

- JDK：8 或更高版本（目标字节码为 Java 8）。
- 构建工具：Maven。
- 执行 `mvn -DskipTests clean package` 生成插件。
- 默认版本号由 `pom.xml` 中的 `revision` 属性控制；构建时可覆盖，例如 `mvn -Drevision=1.9.1 -DskipTests clean package`。

### 兼容性

- 支持核心：Bukkit、Spigot、Paper、Purpur、Folia。
- 目标版本：Minecraft 1.12+。
- 说明：当前优先实现 Bukkit/Paper 系；Sponge 等核心将在后续以独立适配层接入，并尽量保证同一个 `.jar` 的兼容性。

### 架构与性能

- 模块化：核心逻辑、命令、监听、配置、存储分包。
- 事件：保护、解绑、传输可通过内部事件管线扩展（后续开放）。
- 性能：
  - 传输调度尽量保持主线程安全。
  - 分页处理与上限控制，避免一次性搬运过大。
  - 容器位置缓存，减少重复计算。
- 指标：单次传输尽量控制在 50ms 内；基础运行内存小于 50MB；可同时处理 100+ 容器绑定。

### 安全

- 验证容器所有权、阻止外部提取，降低复制风险。
- 权限与命令参数严格校验。

### API（预留）

- 后续将提供事件与接口（绑定、解绑、传输）供其他插件集成。

### 变更日志

- `1.9`：包名迁移至 `org.endlessmc.invchest`，更新作者与项目地址，补充 MIT 许可证，完善 `.gitignore`，清理死代码并修复区块加载与传输上限等问题；语言精简为 `zh_cn`、`zh_tw`、`zh_hk`、`en_us`。
- `0.1.0`：初始版本，包含绑定/解绑、传输策略、容器保护、多语言与配置。

### 许可证

本项目使用 MIT 许可证，详见 [LICENSE](LICENSE)。

Copyright (c) 2024-2026 EndlessMC Organization

---

## English (US)

### Overview

InvChest is a plugin for Bukkit/Spigot/Paper/Purpur/Folia servers. It lets players bind a container and automatically transfer its contents into the player's inventory at a configurable interval. The plugin supports localization and configurable transfer strategies, with architecture reserved for future Sponge, (Neo)Forge, and Fabric support.

### Features

- **Container binding**: bind and unbind containers with commands, including admin management for other players.
- **Automatic transfer**: move container contents into player inventories at a configurable interval and strategy.
- **Container protection**: prevent other players from opening or breaking bound containers, and block external extraction.
- **Localization**: built-in Simplified Chinese, Traditional Chinese (Taiwan/Hong Kong), and English (US).
- **Extensible architecture**: modular design with reserved adapters for Sponge, (Neo)Forge, and Fabric.

### Requirements

- Server cores: Bukkit, Spigot, Paper, Purpur, and Folia.
- Minecraft version: 1.12+.
- Java: 8 or newer (the project targets Java 8 bytecode).
- Build tool: Maven (build only).

### Installation

- Place the built `InvChest.jar` into the server's `plugins/` directory.
- On first startup, `config.yml` and the `lang/` directory are generated (built-in languages: `zh_cn`, `zh_tw`, `zh_hk`, and `en_us`).
- To change the language, set `lang` in `config.yml`.

### Configuration (`config.yml`)

- `transfer-interval`: transfer interval in seconds, default `30`.
- `transfer-mode`: `all` or `try`.
  - `all`: move as many container items into the player's inventory as possible; items that do not fit are dropped at the player's location; when the per-transfer limit is reached, the rest stays in the container for the next cycle.
  - `try`: move items in order until an item cannot be accepted; the remaining items stay in the container.
- `max-items-per-transfer`: maximum number of items per transfer (sum of stack sizes; applies to both `all` and `try` modes).
- `lang`: `zh_cn`, `zh_tw`, `zh_hk`, `en_us`, or `auto_en_us` (the console uses the specified language, while players are matched by their client locale).
- `bind.max-per-player`: maximum number of containers per player (`0` means unlimited).
- `bind.default-chunk-range`: default chunk force-load range (`-1` disabled, `0` current chunk only, `>0` radius).
- `bind.auto-name-format`: automatic name format; `{number}` is replaced with an index.
- `chunk-loading.enabled`: enable or disable chunk force-loading.
- `chunk-loading.max-range`: maximum allowed chunk force-load range.
- `ui.list-items-per-page`: number of entries per page in list commands.
- `transfer-message-enabled`: whether to send a message when items are transferred.

### Commands

- `/invchest bind [name] [chunk-range]` or `/ic bind ...`: bind the container the player is looking at.
- `/invchest unbind [name]` or `/ic unbind ...`: unbind; if no name is given, unbind the container the player is looking at.
- `/invchest list [page]` or `/ic list [page]`: list your bound containers.
- `/invchest lock <name> <true|false>` or `/ic lock ...`: control whether other players may open the container.
- `/invchest start|stop [name]` or `/ic start|stop ...`: start or pause your transfers; if no name is given, apply to all bindings.
- Admin commands:
  - `/invchest abind <player> [name] [chunk-range]` or `/ic abind ...`: bind a container for another player (the admin must look at the target container).
  - `/invchest aunbind <player> [name]` or `/ic aunbind ...`: unbind a container for another player.
  - `/invchest alist [player|@all] [page]` or `/ic alist ...`: list another player's bindings or all bindings on the server.
  - `/invchest astart|astop <player|@all> [name]` or `/ic astart|astop ...`: start or pause transfers for another player or globally.
  - `/invchest reload` or `/ic reload`: reload the configuration.

### Permissions

- `invchest.bind`: bind, unbind, and list your own containers (enabled by default).
- `invchest.lock`: change the access lock state of your own containers (enabled by default).
- `invchest.statue`: start or pause your own transfers (enabled by default).
- `invchest.abind`: admin bind/unbind/list for other players (OP by default).
- `invchest.astatue`: admin start/stop for other players or globally (OP by default).
- `invchest.inf`: show the container owner in protection messages (OP by default).
- `invchest.reload`: reload the configuration (OP by default).

### Containers and Protection

- Supported containers: chests, trapped chests, barrels, and shulker boxes (double chests and double trapped chests are handled as a single container; copper containers are not supported).
- After binding:
  - Other players are prevented from opening the container.
  - Only the owner or a player with `invchest.abind` may break the protected container.
  - External input (hoppers, droppers, etc.) is allowed, while external extraction (hoppers, hopper minecarts, etc.) is blocked.
  - The binding is removed automatically when the container is broken or no longer exists.

### Languages

- Resource directory: `src/main/resources/lang/`.
- Runtime directory: `plugins/InvChest/lang/`.
- Built-in languages: `zh_cn.yml`, `zh_tw.yml`, `zh_hk.yml`, and `en_us.yml`.
- Adding a language: place `<code>.yml` in the resource directory and rebuild.
- Auto mode: with `lang: auto_<code>`, the console uses `<code>` and players are matched by their client locale; missing keys fall back to `en_us`.

### Building

- JDK: 8 or newer (the project targets Java 8 bytecode).
- Build tool: Maven.
- Run `mvn -DskipTests clean package` to build the plugin.
- The default version is controlled by the `revision` property in `pom.xml`; it can be overridden at build time, for example: `mvn -Drevision=1.9.1 -DskipTests clean package`.

### Compatibility

- Supported cores: Bukkit, Spigot, Paper, Purpur, and Folia.
- Target version: Minecraft 1.12+.
- Note: Bukkit/Paper-family cores are implemented first. Sponge and other cores will be added through separate adapters later, while keeping the same `.jar` as compatible as possible.

### Architecture and Performance

- Modular design: core logic, commands, listeners, configuration, and storage are separated into packages.
- Events: protection, unbinding, and transfer events can be extended through an internal event pipeline (planned).
- Performance:
  - Transfer scheduling is kept as main-thread-safe as possible.
  - Paging and per-transfer limits avoid moving too much data at once.
  - Container location caching reduces repeated calculations.
- Targets: keep each transfer under 50 ms when possible; baseline memory usage below 50 MB; support 100+ bound containers.

### Security

- Container ownership is verified and external extraction is blocked to reduce duplication risks.
- Permissions and command arguments are validated strictly.

### API (Planned)

- Events and interfaces (bind, unbind, transfer) will be provided for other plugins to integrate with.

### Changelog

- `1.9`: migrated the package to `org.endlessmc.invchest`, updated authors and project URL, added the MIT license, improved `.gitignore`, removed dead code, and fixed chunk-loading and transfer-limit issues; languages are now limited to `zh_cn`, `zh_tw`, `zh_hk`, and `en_us`.
- `0.1.0`: initial release with bind/unbind, transfer strategies, container protection, localization, and configuration.

### License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

Copyright (c) 2024-2026 EndlessMC Organization

---

## 致谢 / Thanks

- Bukkit/Spigot/Paper 社区 / community
- Purpur/Folia 项目 / projects
- Trae 提供的几乎*完美*的 IDEA / for the almost *perfect* IDE
- CloseAI 的 GPT-5-high
- pama1234 的经济支持 / financial support
