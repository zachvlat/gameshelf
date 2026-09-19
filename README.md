<p align="center">
  <img src="https://raw.githubusercontent.com/zachvlat/gamelibrary/refs/heads/android-native-nonplaynite/app/src/main/play_store_512.png" alt="GameLibrary logo" height="80" />
</p>
<h1 align="center">GameShelf</h1>

<p align="center">
  <a href="https://apps.obtainium.page/redirect?r=obtainium://app/%7B%22id%22%3A%22com.zachvlat.gamelibrary%22%2C%22url%22%3A%22https%3A%2F%2Fgithub.com%2Fzachvlat%2Fgameshelf%22%2C%22author%22%3A%22zachvlat%22%2C%22name%22%3A%22GameShelf%22%2C%22preferredApkIndex%22%3A0%2C%22additionalSettings%22%3A%22%7B%5C%22includePrereleases%5C%22%3Atrue%2C%5C%22fallbackToOlderReleases%5C%22%3Atrue%2C%5C%22filterReleaseTitlesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22filterReleaseNotesByRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22verifyLatestTag%5C%22%3Afalse%2C%5C%22sortMethodChoice%5C%22%3A%5C%22date%5C%22%2C%5C%22useLatestAssetDateAsReleaseDate%5C%22%3Afalse%2C%5C%22releaseTitleAsVersion%5C%22%3Afalse%2C%5C%22trackOnly%5C%22%3Afalse%2C%5C%22versionExtractionRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22matchGroupToUse%5C%22%3A%5C%22%5C%22%2C%5C%22versionDetection%5C%22%3Atrue%2C%5C%22releaseDateAsVersion%5C%22%3Afalse%2C%5C%22useVersionCodeAsOSVersion%5C%22%3Afalse%2C%5C%22apkFilterRegEx%5C%22%3A%5C%22%5C%22%2C%5C%22invertAPKFilter%5C%22%3Afalse%2C%5C%22autoApkFilterByArch%5C%22%3Atrue%2C%5C%22appName%5C%22%3A%5C%22%5C%22%2C%5C%22appAuthor%5C%22%3A%5C%22%5C%22%2C%5C%22shizukuPretendToBeGooglePlay%5C%22%3Afalse%2C%5C%22allowInsecure%5C%22%3Afalse%2C%5C%22exemptFromBackgroundUpdates%5C%22%3Afalse%2C%5C%22skipUpdateNotifications%5C%22%3Afalse%2C%5C%22about%5C%22%3A%5C%22%5C%22%2C%5C%22refreshBeforeDownload%5C%22%3Afalse%2C%5C%22includeZips%5C%22%3Afalse%2C%5C%22zippedApkFilterRegEx%5C%22%3A%5C%22%5C%22%7D%22%2C%22overrideSource%22%3Anull%7D">
    <img src="https://raw.githubusercontent.com/jiangtian616/JHenTai/master/badges/get_it_on_obtainium.png" alt="Get it on Obtainium" height="60">
  </a>
</p>

<br>

<p align="center">
<img width="200" alt="1" src="https://github.com/user-attachments/assets/3b73e958-bacc-430c-af98-d6a716891e5b" />
<img width="200" alt="2" src="https://github.com/user-attachments/assets/9c3bb567-9621-4e7d-a10e-78a91174435c" />
<img width="200" alt="3" src="https://github.com/user-attachments/assets/a36f9b51-ff6e-4e1b-8484-641968d49d9b" />
<img width="200" alt="4" src="https://github.com/user-attachments/assets/521b2fa5-1cdb-4c65-b6d8-fd704592707a" />

</p>




<p align="center">
An Android app that aggregates your owned game libraries from Epic Games Store, GOG, and Amazon Gaming into one place.
Built with Jetpack Compose, Ktor, Room, and Material 3 dynamic colors. Auth is handled via in-app WebView popup. Games are cached locally with 7-day TTL.
</p>

## Stores Supported

- **Epic Games** – OAuth via WebView + entitlement API
- **GOG** – OAuth via WebView + Galaxy API
- **Amazon Gaming** – OAuth via WebView + entitlements API (device-auth flow)
- **Steam** – JS scrape via WebView (prompts for username, scrapes games page)
- **Itch.io** - Scrape via WebView
