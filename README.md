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

## Commands

- `/ah`
- `/ah <page>`
- `/ah sell <price>`
- `/ah search <name>`
- `/ah expired`
- `/ah claim`
- `/ah reload`

## Permissions

- `auctionhouse.use`
- `auctionhouse.sell`
- `auctionhouse.buy`
- `auctionhouse.unlimited`
- `auctionhouse.admin`

Compatibility aliases are also included:

- `donutauction.use`
- `donutauction.sell`
- `donutauction.buy`
- `donutauction.unlimited`
- `donutauction.admin`

## Requirements

- Paper `1.21.11`
- Java `21`
- Vault
- an economy provider such as EssentialsX Economy or CMI

## Configuration

Main settings live in `src/main/resources/config.yml`.

- `currency-symbol`: symbol shown in menus and messages
- `currency-format`: number format pattern
- `max-listings-per-player`: listing cap without unlimited permission
- `min-price` / `max-price`: sell price limits
- `auction-duration`: listing lifetime such as `48h`

Runtime data is stored in:

- `auctions.yml`
- `expired.yml`

## Build

From the project root:

```powershell
mvn clean package
```

Output:

- `target/donut-auction-house-1.0.0.jar`

## Install

1. Build the plugin with Maven.
2. Copy `target/donut-auction-house-1.0.0.jar` into your server `plugins/` folder.
3. Install Vault and an economy provider.
4. Start or restart the server.
5. Adjust the generated plugin config if needed.

## Project Structure

- `src/main/java/bg/nikol/auctionhouse/` core plugin logic
- `src/main/resources/` plugin metadata and defaults
- `pom.xml` Maven build configuration

## Status

The repo is source-only. Built `.jar` files, Maven output, and portable toolchains are intentionally excluded from version control.
