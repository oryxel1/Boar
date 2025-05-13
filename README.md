# Boar

Boar is a POC project that allows you to enable [server-auth-with-rewind](https://github.com/Mojang/bedrock-protocol-docs/blob/main/additional_docs/ConfiguringAntiCheat.md) for 
[GeyserMC](https://github.com/GeyserMC/Geyser) project with a few more checks and improvements compare to BDS.

### Documented differences between Java - Bedrock [here](https://github.com/Oryxel/Boar/blob/new-engine/DIFFERENCES_WIKI.md) with detailed explainations.

### ⚠️ WARNING: THIS ONLY FOR BEDROCK PLAYER NOT JAVA PLAYER! YOU WILL NEED TO PAIR THIS WITH ANOTHER JAVA ANTICHEAT!
A dedicated (proof of concept) anti cheat for GeyserMC project.
- Warning: No guarantee about performance, lag compatibility, or if I will ever finish this.

### Features
- I will keep this short: lag compensation, movement simulation (prediction), smooth rewind setback.
- Also this anticheat is actually a Geyser extension!

### Problems
- A lot of movement differences (and features) is not implemented.

### Differences from other Geyser anti-cheat.
#### You can take a look at a list of "other anti-cheats" [here](https://geysermc.org/wiki/geyser/anticheat-compatibility/)
- It's free..... and open-source, and it's a prediction ac, match BDS rewind stuff, the list go on.
- Fully packet-based that read and interfere directly (if needed) bedrock packet that client/server sent.
- Fully prediction-based, check player movement on a 1:1 scale, detecting any *1.0E-4* movement abnormality.
- Fully synced to the player ping to account for lag.

### Credits
- https://github.com/GeyserMC/Geyser
- https://github.com/oomph-ac/oomp
- https://github.com/RaphiMC/ViaBedrock
- https://github.com/Mojang/bedrock-protocol-docs