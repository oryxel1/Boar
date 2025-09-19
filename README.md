# Boar

Boar is a POC project that allows you to enable [server-auth-with-rewind](https://github.com/Mojang/bedrock-protocol-docs/blob/main/additional_docs/ConfiguringAntiCheat.md) for 
[GeyserMC](https://github.com/GeyserMC/Geyser) project with a few more checks and improvements compare to BDS.

* *Note: I'm getting more and more tired maintaining this project, so this project won't be frequently update. However this project is not abandoned, I will still maintain
it, update and push fixes from time to time if needed. If there are bugs and bypasses, you can still can contact to me about it and I wil get to it when I can.*

### Documented differences between Java - Bedrock [here](https://github.com/Oryxel/Boar/blob/new-engine/DIFFERENCES_WIKI.md) with detailed explainations.

### ⚠️ WARNING: THIS ONLY FOR BEDROCK PLAYER NOT JAVA PLAYER! YOU WILL NEED TO PAIR THIS WITH ANOTHER JAVA ANTICHEAT!
A dedicated (proof of concept) anti cheat for GeyserMC project.
- Warning: No guarantee about performance, lag compatibility, or if I will ever finish this.

### Features
- I will keep this short: lag compensation, movement simulation (prediction), smooth rewind setback.
- Also this anticheat is actually a Geyser extension!

### Current detections list
#### Almost every single movement-related cheats (except vehicle aka boat/horse), including - but not limited to:
- Fly, Jesus, Step, Fast Climb, High Jump (Any type of fly cheats)
- Speed (Any type of speed cheats)
- No Fall (Detected using the fly check, impossible to bypass)
- Velocity (99.99%/100.01% velocity - basically any kind of velocity cheat)
- No Slow
- And the list goes on....
#### And other additions check aside from movements.
- Reach (> 3 blocks, depends on the config)
- Hitbox (any kind of hitbox expansion, including touch (cheater) player)
- Timer (anything greater than 20 ticks)
- PingSpoof (cannot guarantee a 100% detection, but can silently compensate for some)

### Problems
- A lot of movement differences (and features) is not implemented.

### Differences from other Geyser anti-cheat.
#### You can take a look at a list of "other anti-cheats" [here](https://geysermc.org/wiki/geyser/anticheat-compatibility/)
- It's free..... and open-source, which is pretty dang good already.
- Boar is **extremly sensitive** and can detect **EXTREMLY** small movement mismatch, designed based off the vanilla movement code making it *mathematically impossible to bypass*. 
- Boar can accurately detect and cancel any hit **beyond 3.0 blocks reach** with minimal falses while not affecting legit player but still accurately cancelling cheaters hit.
- Can effectively detect **pingspoofing** by abusing a certain system in RakNet, and silently compenstating for it, making it harder to use lag-based cheats.
- "Perfectly" account for **client lag and latency lag** without relying on tricks, making it harder to false lagging legits player while still effectively catching cheaters. 
- Accurately check for any timer-based check, even if the cheaters only move 1 ticks faster (**1.001x - 1.05x game speed**) boar can still catch and detect it, without affecting lagging players.

### Credits
- https://github.com/GeyserMC/Geyser
- https://github.com/oomph-ac/oomph (fireworks boosting boost code)
- https://github.com/RaphiMC/ViaBedrock
- https://github.com/Mojang/bedrock-protocol-docs
