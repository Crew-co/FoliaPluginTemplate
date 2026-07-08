# Folia 1.21.11 Kotlin Plugin Template

A ready-to-build [Folia](https://github.com/PaperMC/Folia) plugin template for **Minecraft 1.21.11**, written in **Kotlin** with the **Gradle Kotlin DSL**. It ships with an annotation-driven command framework and Folia-aware scheduler helpers so you can skip the boilerplate and start writing features.

- Target: `dev.folia:folia-api:1.21.11-R0.1-SNAPSHOT` (Java 21)
- Build: Gradle (Kotlin DSL) + Shadow (fat jar with the Kotlin runtime bundled)
- Descriptor: `plugin.yml` generated from the build by [plugin-yml](https://github.com/Minecrell/plugin-yml) — nothing to hand-edit
- Testing: `runServer` / `runFolia` tasks from [run-paper](https://github.com/jpenilla/run-task) download a server and launch your plugin in one command
- Commands: declare them with `@Command` / `@Subcommand` / `@TabComplete` — no `commands:` block to maintain
- Events: plain Bukkit listeners with a worked example (join message, block-break reaction)
- GUIs: a small Folia-safe chest-menu framework (holder-based, no shared registry) with a worked example
- Scheduling: `Schedulers` helper that wraps Folia's region / entity / async schedulers (there is no main thread on Folia)
- Config & messages: hot-reloadable `config.yml` / `messages.yml`, parsed off-thread into immutable snapshots
- Extras: thread-safe cooldowns, typed PersistentDataContainer helpers, an async GitHub-releases update checker, and bStats metrics (Folia-safe, relocated)
- Quality: JUnit tests for the pure logic, a GitHub Actions build, and an `.editorconfig`

---

## Quick start

1. **Requirements:** JDK 21 (`java -version` should say 21). No global Gradle needed — the wrapper handles it.
2. **Build:**
   ```bash
   ./gradlew build        # macOS/Linux
   gradlew.bat build      # Windows
   ```
3. **Grab the jar:** `build/libs/folia-template-1.0.0.jar`
4. **Test it without installing anything** — this downloads a Folia server and launches it with your plugin already loaded:
   ```bash
   ./gradlew runFolia     # a real Folia 1.21.11 server, plugin loaded
   ./gradlew runServer    # or a plain Paper server, for comparison
   ```
   Type `stop` in that console to shut it down. The server files live under `run/` (git-ignored).
5. **Or install manually:** drop the jar into your own Folia server's `plugins/` folder and start it.
6. **Try it in-game:** `/example ping`

---

## Make it yours

Change these, then rebuild:

| What | Where |
|------|-------|
| Plugin name / version / group | `gradle.properties` (version/group) and the `bukkit { }` block in `build.gradle.kts` (display name) |
| Java package (`com.crewco.foliatemplate`) | rename the folders under `src/main/kotlin/...` and update `main` in the `bukkit { }` block |
| Minecraft / Folia version | `foliaApiVersion` in `gradle.properties` (the `runServer`/`runFolia` Minecraft version is derived from it automatically) |
| Author, description, api-version | the `bukkit { }` block in `build.gradle.kts` |

There is no `plugin.yml` file to edit — plugin-yml generates it from the `bukkit { }` block at build time, and the version/name come straight from Gradle, so you set each thing in exactly one place.

---

## Project layout

```
folia-template/
├── .github/workflows/build.yml # CI: builds + tests on every push/PR
├── .editorconfig               # shared formatting rules
├── build.gradle.kts            # deps, toolchain, shadow jar, plugin.yml, run tasks
├── settings.gradle.kts         # project name
├── gradle.properties           # versions + coordinates (edit these)
├── gradlew / gradlew.bat       # Gradle wrapper — no local Gradle install needed
└── src/
    ├── main/
    │   ├── kotlin/com/example/foliatemplate/
    │   │   ├── FoliaTemplatePlugin.kt   # entry point (onEnable/onDisable)
    │   │   ├── command/                 # annotation command framework
    │   │   ├── gui/                     # Folia-safe chest-menu framework
    │   │   ├── scheduler/Schedulers.kt  # Folia region/entity/async helpers
    │   │   ├── config/                  # Config.kt + Messages.kt (hot-reload)
    │   │   ├── pdc/PersistentData.kt     # PersistentDataContainer helpers
    │   │   ├── update/UpdateChecker.kt   # async GitHub-release version check
    │   │   ├── util/                     # Text (mm), Cooldowns
    │   │   ├── commands/ExampleCommand.kt   # worked command example
    │   │   ├── listeners/ExampleListener.kt # worked event example
    │   │   └── menus/ExampleMenu.kt         # worked GUI example
    │   └── resources/
    │       ├── config.yml              # default config
    │       └── messages.yml            # default messages
    └── test/kotlin/com/example/foliatemplate/
        ├── CooldownsTest.kt            # pure-logic unit tests
        └── VersionCompareTest.kt
```

There's no `resources/plugin.yml` — the descriptor (including `folia-supported: true`) is generated into `build/generated/plugin-yml/` and folded into the shaded jar.

## Gradle plugins & tasks

The build applies four plugins, each doing one job:

- **`org.jetbrains.kotlin.jvm`** — compiles the Kotlin sources.
- **`com.gradleup.shadow`** — builds the fat jar (`shadowJar`) with the Kotlin runtime inside. This is the artifact you ship.
- **`net.minecrell.plugin-yml.bukkit`** — generates `plugin.yml` from the `bukkit { }` block, so the descriptor can never drift from your build config. It also supports a `libraries` list (runtime library loading via Paper's library loader) if you'd rather not shade some dependencies.
- **`xyz.jpenilla.run-paper`** — adds `runServer` (Paper) and `runFolia` (Folia) tasks that download the server, drop your jar in, and launch it. Great for a tight edit-build-test loop. Both are pinned to Java 21 so they match the target runtime. Because our code uses the region/entity/async schedulers (which Paper also implements), it runs on both — handy for spotting Folia-only threading issues by diffing the two.

---

## How the command framework works

You write a plain class, annotate it, and register one instance. `CommandManager` reads the annotations by reflection, builds a Bukkit command, and inserts it into the server's command map. There is **no `commands:` section in `plugin.yml`** — the annotation *is* the source of truth.

### The annotations

- **`@Command`** (on the class) — declares the command name, aliases, permission, description, and whether it's player-only.
- **`@Default`** (on a method) — runs when the command is used with no matching subcommand, e.g. bare `/example`.
- **`@Subcommand("name")`** (on a method) — runs when the first argument matches. Supports `aliases`, a per-subcommand `permission`, `playerOnly`, `minArgs` (with a `usage` hint shown when too few args are given).
- **`@TabComplete(subcommand = "name")`** (on a method returning `List<String>`) — supplies tab suggestions. Leave `subcommand` empty to complete the first token; the framework already suggests subcommand names automatically.

Every handler/tab method takes a single `CommandContext`.

### A minimal command

```kotlin
@Command(name = "hello", permission = "myplugin.hello")
class HelloCommand {

    @Default
    fun run(ctx: CommandContext) {
        ctx.success("Hello, ${ctx.sender.name}!")
    }

    @Subcommand("wave", playerOnly = true)
    fun wave(ctx: CommandContext) {
        ctx.reply("<yellow>*waves*</yellow>")
    }
}
```

Register it in `onEnable`:

```kotlin
commands.register(HelloCommand())
```

That's the whole loop: annotate → register → done.

### CommandContext helpers

`CommandContext` keeps handlers short:

- **Access:** `arg(i)`, `argOr(i, default)`, `joinFrom(i)`, `size`, `isEmpty`
- **Typed (nullable):** `intArg(i)`, `doubleArg(i)`, `boolArg(i)`, `playerArg(i)`
- **Typed (throwing, with a friendly message):** `requireInt(i)`, `requireDouble(i)`, `requirePlayer(i)`, `requireSenderPlayer()`
- **Reply:** `reply(...)`, `success(...)`, `error(...)`, `info(...)` — strings are parsed as [MiniMessage](https://docs.advntr.dev/minimessage/format.html), so `"<green>done</green>"` works
- **Sender:** `sender`, `player` (nullable), `hasPermission(perm)`

Throw with `fail("message")` (or the `require*` helpers) anywhere in a handler to abort and message the sender — `CommandManager` catches it for you.

---

## How scheduling works on Folia

**Folia has no main thread.** The world is split into regions that each tick on their own thread, so the old `Bukkit.getScheduler().runTask(...)` pattern is gone for anything touching world state. You schedule onto the thread that *owns* what you want to change. The `Schedulers` helper (available as `plugin.schedulers`) wraps this:

| Helper | Runs on | Use it for |
|--------|---------|------------|
| `global { }` | global region thread | world-wide state: time, weather, global data |
| `region(location) { }` | region owning that location | placing/breaking blocks, effects at a spot |
| `entity(entity) { }` | region owning that entity | teleporting/modifying a player or mob |
| `async { }` | background thread | HTTP, file, or database I/O — **never** touch world state here |

Each has `...Delayed(...)` and `...Repeating(...)` variants. Delays are in **ticks** (20 ticks = 1 second) except the async helpers, which take a real duration + `TimeUnit`.

The common pattern — do slow work off-thread, then apply the result safely:

```kotlin
plugin.schedulers.async {
    val data = fetchFromApi()               // background thread: safe for I/O
    plugin.schedulers.entity(player) {
        player.sendMessage(data.toString())  // back on the player's region thread
    }
}
```

`entity(...)` also takes an optional `retired` callback that fires if the entity is gone (e.g. the player logged out) before the task runs, so you can clean up.

`Schedulers.isFolia` tells you at runtime whether you're on Folia, in case you want a build that also degrades gracefully on plain Paper.

---

## Thread safety & async — read this

On Folia "thread-safe" has a precise meaning, and the template is built around it.

**What's already safe.** Commands are called on the region that owns the sender (console commands run on the global region). So inside a handler you may freely read/modify the sender, their location, and nearby blocks — you're already on the correct thread. The command framework itself holds no mutable state after startup (the annotation index is built once and then read-only), so it's safe to drive from all region threads in parallel.

**What is *not* automatically safe: your own data.** Folia only guarantees that a region owns its chunks/entities while it ticks. It guarantees nothing about fields, maps, or caches your plugin keeps. Because different regions run commands and events on different threads at the same time, any shared mutable state you add must be made safe by you. Three options, cheapest first:

1. **Confine it** — only ever touch that state from one place, e.g. inside `schedulers.global { }`, so it's effectively single-threaded.
2. **Use concurrent types** — `ConcurrentHashMap`, `AtomicInteger`/`AtomicReference`, `LongAdder`. The `counter` subcommand in `ExampleCommand.kt` shows this: a `ConcurrentHashMap<UUID, AtomicInteger>` that's correct without any locking.
3. **Lock** — a plain `synchronized` block or a `ReentrantLock` for compound updates that concurrent collections can't express atomically.

A `ConcurrentHashMap` alone is *not* a fix-all — read-modify-write across two calls still races. Use `compute`/`computeIfAbsent`/atomics for that, as the example does.

**The async pattern.** For anything blocking — HTTP, database, disk — never do it on a region thread (you'd stall that region's tick). Push it to `schedulers.async { }`, then hop back onto an owning region to apply the result. You cannot assume you're still on the sender's thread after going async, so route the reply explicitly:

```kotlin
val sender = ctx.sender                    // capture before leaving the thread
plugin.schedulers.async {
    val data = fetchFromApi()              // safe: off any region thread
    plugin.schedulers.forSender(sender) {  // back on the sender's owning region
        sender.sendMessage(data.render())  // safe to touch the game again
    }
}
```

`forSender(...)` routes to the player's entity scheduler (or the global region for console) and quietly drops the task if the player has logged off — so you never touch a player on the wrong thread. The `count` subcommand demonstrates the full loop.

**Handy rules baked into the helpers.**

- Moving an entity → `schedulers.entity(entity) { entity.teleportAsync(target) }`. Always `teleportAsync`, never the blocking `teleport`, and always on the entity's region.
- Editing the world at a spot → `schedulers.region(location) { ... }`.
- Not sure you're on the right thread (e.g. inside someone else's callback)? Check with `schedulers.ownsRegion(location)` / `schedulers.ownsEntity(entity)` before touching state, and schedule if it returns false.
- Don't use `Bukkit.getScheduler()` — it's deprecated on Folia and will warn or fail.

---

## Events

Event listeners are plain Bukkit: implement `Listener`, annotate handlers with `@EventHandler`, and register once in `onEnable`:

```kotlin
server.pluginManager.registerEvents(ExampleListener(this), this)
```

The Folia rule is the same as for commands: **a handler runs on the region that owns the event's player/block**, so touching that player or nearby blocks inside the handler is already thread-safe. You only need a scheduler when you want to reach a *different* entity or location than the one the event is about. `ExampleListener.kt` shows a `PlayerJoinEvent` welcome (plus a delayed follow-up on the player's own region, with a `retired` callback for the log-out case) and a `BlockBreakEvent` that reacts to breaking diamond ore.

## GUIs

The `gui/` package is a tiny, Folia-safe chest-menu framework. Subclass `Menu`, place buttons in `build`, and open it:

```kotlin
class ConfirmMenu(schedulers: Schedulers) : Menu(schedulers, mm("<red>Are you sure?"), rows = 1) {
    override fun build(player: Player) {
        button(3, icon(Material.LIME_DYE, "<green>Yes")) { ctx ->
            ctx.reply("<green>Confirmed!")
            ctx.close()
        }
        button(5, icon(Material.RED_DYE, "<red>No")) { ctx -> ctx.close() }
        fill(icon(Material.GRAY_STAINED_GLASS_PANE, " "))
    }
}

// open it (e.g. from a command):
ConfirmMenu(plugin.schedulers).open(player)
```

How it stays safe and simple:

- **Identification is holder-based.** Each `Menu` *is* its own inventory's `InventoryHolder`, so `MenuListener` recognises a menu with `topInventory.holder is Menu`. There's no global "open menus" map to synchronize — the single biggest source of GUI thread-bugs on Folia is avoided by construction.
- **Opening is scheduled.** `open(player)` runs `build` and `openInventory` on the player's region via the entity scheduler, because opening an inventory must happen on the owning region.
- **Clicks are cancelled and dispatched.** `MenuListener` cancels the whole click event (so items can't be taken or shift-clicked in) and calls your button's lambda with a `ClickContext` (the player, slot, click type, helpers like `close()` and `reply()`). Inventory events fire on the player's region, so the handler is already on the right thread.
- **One instance per open.** Create a fresh `Menu` each time you open it for a player (as `ExampleMenu`/`/example menu` do). That confines a player's menu state to their region thread — no locks needed. Don't share a single instance across players unless you synchronize it yourself.

`icon(material, name, vararg lore)` builds items with MiniMessage names/lore and italics disabled. `/example menu` opens `ExampleMenu` as a live demo.

## Config & messages

`config.yml` and `messages.yml` are copied to the data folder on first run. Both are loaded through the same Folia-safe pattern: the parsed result is an **immutable snapshot** held in an `AtomicReference`. Region threads only ever do a cheap read of the current snapshot (`plugin.config.current`), never disk I/O. Reloads parse the file on the async scheduler and swap the whole snapshot in one atomic write, so a reload can't tear a half-updated state across threads.

```kotlin
if (plugin.config.current.welcomeEnabled) { ... }
player.sendMessage(plugin.messages.component("welcome", "player" to player.name))
```

`Config.Settings` is a typed `data class` — add fields there and read them in `read()`. `messages.yml` values are MiniMessage with `{placeholder}` substitution; missing keys render visibly instead of throwing. `/example reload` reloads both live (no restart).

## Extras

- **Cooldowns** (`util/Cooldowns.kt`) — thread-safe per-player cooldowns backed by a `ConcurrentHashMap`; `test(id, duration)` does an atomic check-and-start so it can't race across regions. `/example boom` uses it, with the duration read from config. The clock is injectable, which is what makes it unit-testable.
- **Persistent data** (`pdc/PersistentData.kt`) — typed `getString`/`setInt`/`setBoolean`/… extensions over the PersistentDataContainer, Bukkit's built-in durable storage (survives restarts, no database). `/example note <text>` stores a note on the player. PDC on a live entity must be touched on that entity's region — a handler for that entity already is.
- **Update checker** (`update/UpdateChecker.kt`) — checks a GitHub repo's latest release on the async scheduler and logs if a newer version exists. Read-only, never self-updates. Set `GITHUB_REPO` (`"owner/repo"`) in the plugin class (or remove the call). It uses the `/releases/latest` endpoint (drafts/pre-releases excluded) and compares the release tag; version comparison is a pure, unit-tested function.
- **Metrics** — bStats (3.1.0, which is Folia-aware) is bundled and relocated. Set `BSTATS_PLUGIN_ID` to your id from [bstats.org](https://bstats.org/getting-started); it stays disabled while `0`.

## Testing & CI

Pure logic (cooldowns, version comparison) is covered by JUnit 5 tests in `src/test`; run them with `./gradlew test` (they also run as part of `build`). Server-dependent behaviour isn't unit-tested here — MockBukkit doesn't model Folia's regionized scheduling, so the honest place to exercise that is a real server via `runFolia`. The GitHub Actions workflow (`.github/workflows/build.yml`) builds and tests on every push/PR and uploads the jar as an artifact, so forks get a green-check gate for free.

## Optional next steps (deliberately not pre-wired)

Two of my own suggestions I left as opt-in rather than baking in, because each carries a real risk I couldn't verify without a live build and would rather you enable on purpose:

**Brigadier commands.** Paper's modern command API gives typed arguments and client-side suggestion validation. It's marked experimental, so I didn't want unverified experimental-API code able to break the whole build. To try it alongside the existing system:

```kotlin
// build: nothing extra — Brigadier ships with the Folia API.
@Suppress("UnstableApiUsage")
override fun onEnable() {
    lifecycleManager.registerEventHandler(io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS) { event ->
        val root = io.papermc.paper.command.brigadier.Commands.literal("brigtest")
            .then(io.papermc.paper.command.brigadier.Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                .executes { ctx ->
                    val amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount")
                    ctx.source.sender.sendMessage("amount = $amount")
                    com.mojang.brigadier.Command.SINGLE_SUCCESS
                })
        event.registrar().register(root.build())
    }
}
```

**Linting.** Add [detekt](https://detekt.dev) for static analysis and/or [ktlint](https://github.com/JLLeitschuh/ktlint-gradle) for formatting:

```kotlin
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.7"   // pick versions compatible with your Gradle/Kotlin
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}
detekt { buildUponDefaultConfig = true; ignoreFailures = true } // tighten once you have a baseline
```

The `.editorconfig` is already in place and both tools honour it.

## The example command

`ExampleCommand.kt` exercises every piece:

| Command | Shows |
|---------|-------|
| `/example` | `@Default` handler |
| `/example ping` | simplest `@Subcommand` |
| `/example tp <player>` | `playerOnly`, `minArgs`, `requirePlayer`, entity scheduler, `@TabComplete` |
| `/example boom` | region scheduler (world edit at a location) |
| `/example count <n>` | async work hopping back to the sender's region via `forSender` |
| `/example counter` | thread-safe shared state (`ConcurrentHashMap` + `AtomicInteger`) |
| `/example menu` | opens the example GUI (`menus/ExampleMenu.kt`) |
| `/example note [text]` | PersistentDataContainer storage (survives restarts) |
| `/example reload` | hot-reloads config + messages off-thread |

Delete it once you've read it.

---

## Notes & gotchas

- **`folia-supported: true`** is mandatory — Folia refuses to load plugins without it. Here it's set by `foliaSupported = true` in the `bukkit { }` block and generated into `plugin.yml` for you.
- **The Kotlin runtime is bundled** by the Shadow plugin because servers don't ship Kotlin. For a plugin you distribute publicly, uncomment the `relocate("kotlin", ...)` line in `build.gradle.kts` so your bundled Kotlin can't clash with other Kotlin plugins.
- **Use only the shaded jar** (`build/libs/folia-template-1.0.0.jar`). The plain `jar` task is disabled to avoid confusion.
- **First run is slow** — Gradle downloads itself, the plugins, Kotlin, and the Folia API; `runFolia` additionally downloads a server the first time. Everything after is cached.
- **Prefer a `paper-plugin.yml`?** plugin-yml can generate that instead — apply `net.minecrell.plugin-yml.paper` and use a `paper { }` block (it also has `foliaSupported`). Command registration here goes through the command map, so it works either way.
- **Regenerating the wrapper:** if you have a local Gradle, `gradle wrapper --gradle-version 8.11.1` refreshes the wrapper files.
