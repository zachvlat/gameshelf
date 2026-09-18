package com.zachvlat.gamelibrary.library.util

object EpicConstants {
    const val CLIENT_ID = "34a02cf8f4414e29b15921876da36f9a"
    const val CLIENT_SECRET = "daafbccc737745039dffe53d94fc76cf"
    const val OAUTH_HOST = "account-public-service-prod03.ol.epicgames.com"
    const val TOKEN_URL = "https://$OAUTH_HOST/account/api/oauth/token"
    const val LOGIN_URL = "https://www.epicgames.com/id/login"
    const val REDIRECT_URL = "https://www.epicgames.com/id/api/redirect"
    const val LIBRARY_HOST = "library-service.live.use1a.on.epicgames.com"
    const val LIBRARY_URL = "https://$LIBRARY_HOST/library/api/public/items"
    const val CATALOG_HOST = "catalog-public-service-prod06.ol.epicgames.com"
    const val CATALOG_URL = "https://$CATALOG_HOST/catalog/api/shared/namespace"
    const val STORE_BASE_URL = "https://www.epicgames.com/store/en-US/product"
}

object GogConstants {
    const val CLIENT_ID = "46899977096215655"
    const val CLIENT_SECRET = "9d85c43b1482497dbbce61f6e4aa173a433796eeae2ca8c5f6129f2dc4de46d9"
    const val AUTH_URL = "https://auth.gog.com/auth"
    const val TOKEN_URL = "https://auth.gog.com/token"
    const val REDIRECT_URI = "https://embed.gog.com/on_login_success?origin=client"
    const val USERS_API = "https://users.gog.com/users"
    const val GALAXY_LIBRARY_API = "https://galaxy-library.gog.com/users"
    const val GAMESDB_API = "https://gamesdb.gog.com/platforms/gog/external_releases"
    const val STORE_BASE_URL = "https://www.gog.com/en/game"
}

object AmazonConstants {
    const val AMAZON_API = "https://api.amazon.com"
    const val REGISTER_URL = "$AMAZON_API/auth/register"
    const val TOKEN_URL = "$AMAZON_API/auth/token"
    const val DEREGISTER_URL = "$AMAZON_API/auth/deregister"
    const val AUTH_BASE_URL = "https://amazon.com/ap/signin"
    const val ENTITLEMENTS_URL = "https://gaming.amazon.com/api/distribution/entitlements"
    const val MARKETPLACE_ID = "ATVPDKIKX0DER"
    const val DEVICE_TYPE = "A2UMVHOX7UP4V7"
    const val ASSOC_HANDLE = "amzn_sonic_games_launcher"
}

object ItchConstants {
    const val PURCHASES_URL = "https://itch.io/my-purchases"
    const val STORE_BASE_URL = "https://itch.io"
}

object EaConstants {
    const val ORDER_HISTORY_URL = "https://myaccount.ea.com/am/data/1/order-history?dateRange=ALL"
    const val STORE_BASE_URL = "https://www.ea.com/games"
}

object UbisoftConstants {
    const val GAMES_ACTIVITY_URL = "https://www.ubisoft.com/en-gb/account/games-activity"
    const val STORE_BASE_URL = "https://store.ubisoft.com"
}

object IgdbConstants {
    const val TOKEN_URL = "https://id.twitch.tv/oauth2/token"
    const val GAMES_API = "https://api.igdb.com/v4/games"
}
