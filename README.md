# AuctionHouse

Paper `1.21.11` auction house plugin with a DonutSMP-style market GUI, Vault economy support, listing expiration, and claim flow.

## Features

- `/ah` market GUI with paging
- `/ah sell <price>` with suffix support like `1k`, `2.5m`, and `1b`
- `/ah search <name>` item search
- buy confirmation UI
- seller-side removal for owned listings
- expired listing storage with `/ah expired` and `/ah claim`
- YAML-backed persistence
- Vault economy integration

## Requirements

- Paper `1.21.11`
- Java `21`
- Vault
- an economy provider such as EssentialsX Economy or CMI

Important:
- the plugin depends on a working Vault economy provider at startup
- if Vault is missing or no economy provider is registered, the plugin disables itself

## Quick Start

Recommended first-time setup:

1. Install `Vault`.
2. Install one economy plugin such as `EssentialsX Economy` or `CMI`.
3. Build the plugin jar with Maven.
4. Copy `target/donut-auction-house-1.0.0.jar` into `plugins/`.
5. Start the server and confirm the plugin stays enabled.
6. Run `/ah` in-game to verify the GUI opens correctly.
7. Test a full flow:
   - list an item with `/ah sell <price>`
   - buy it from another account
   - check expired storage with `/ah expired` and `/ah claim`

If the plugin disables itself during startup, check Vault first and confirm that your economy provider is loaded before AuctionHouse.

## Commands

- `/ah`
- `/ah <page>`
- `/ah sell <price>`
- `/ah search <name>`
- `/ah expired`
- `/ah claim`
- `/ah reload`

Aliases:
- `/auctionhouse`
- `/auction`

## Permissions

- `auctionhouse.use`
  - Open the auction house GUI and basic player usage.
- `auctionhouse.sell`
  - Create listings with `/ah sell <price>`.
- `auctionhouse.buy`
  - Buy items from the GUI.
- `auctionhouse.unlimited`
  - Bypass the normal listing cap.
- `auctionhouse.admin`
  - Reload the plugin and use admin-level actions.

Compatibility aliases are also included:

- `donutauction.use`
- `donutauction.sell`
- `donutauction.buy`
- `donutauction.unlimited`
- `donutauction.admin`

## Economy Setup

Recommended server-side setup:

1. Install `Vault`.
2. Install one supported economy plugin, for example `EssentialsX Economy` or `CMI`.
3. Start the server and confirm the economy plugin is loaded before AuctionHouse.
4. Add the AuctionHouse jar to `plugins/`.
5. Start the server again and verify the plugin stays enabled.

What the plugin uses Vault for:
- checking whether the buyer has enough balance
- withdrawing money on purchase
- depositing money to the seller
- refunding the buyer if seller payout fails

## Configuration

Main settings live in [src/main/resources/config.yml](src/main/resources/config.yml).

### Main Options

- `currency-symbol`
  - Symbol shown in GUI text and messages. Default: `$`
- `currency-format`
  - Java number format pattern used for prices. Default: `#,##0.##`
- `max-listings-per-player`
  - Maximum active listings per player unless they have `auctionhouse.unlimited`. Default: `20`
- `min-price`
  - Lowest allowed sell price. Default: `1`
- `max-price`
  - Highest allowed sell price. Default: `1000000000`
- `auction-duration`
  - Listing lifetime. Supports `s`, `m`, `h`, and `d`. Default: `48h`

### Data File Options

- `files.listings-file`
  - File used for active listings. Default: `auctions.yml`
- `files.expired-file`
  - File used for expired listings waiting to be claimed. Default: `expired.yml`

### Runtime Data

Runtime data is stored in:

- `auctions.yml`
- `expired.yml`

## Install

1. Build the plugin with Maven.
2. Copy `target/donut-auction-house-1.0.0.jar` into your server `plugins/` folder.
3. Install Vault and an economy provider.
4. Start the server once so the plugin can generate its config and data files.
5. Adjust the generated config if needed.
6. Reload the plugin with `/ah reload` or restart the server.

## Build

From the project root:

```powershell
mvn clean package
```

Output:

- `target/donut-auction-house-1.0.0.jar`

## Project Metadata

- [pom.xml](pom.xml)
  - Maven artifact metadata, dependency declarations, Java toolchain target, and repository information.
- [src/main/resources/plugin.yml](src/main/resources/plugin.yml)
  - Bukkit command, permission, and plugin metadata used by Paper at runtime.

## Project Structure

- `src/main/java/bg/nikol/auctionhouse/`
  - Core plugin logic, command handling, GUI flow, persistence, and listeners.
- `src/main/resources/`
  - Plugin metadata and default configuration.
- `pom.xml`
  - Maven build configuration with Paper and VaultAPI as provided dependencies.

## Status

The repo is source-only. Built `.jar` files, Maven output, and portable toolchains are intentionally excluded from version control.

## License

MIT
