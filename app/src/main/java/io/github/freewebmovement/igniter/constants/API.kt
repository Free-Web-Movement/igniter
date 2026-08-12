package io.github.freewebmovement.igniter.constants

object API {
    @JvmField
    var SERVER_LIST_URI = "https://free-web-movement.github.io/assets/servers.json"
    @JvmField
    var API_DOMAIN = "games.yikuaijiasu.top"
    @JvmField
    var BASE_URL = "https://" + API_DOMAIN + "/"
    @JvmField
    var API_QUOTA_PATH = "user/quota"
    var API_URI = "https://\$API_DOMAIN:\$API_PORT/"
    @JvmField
    var API_QUOTA_KEY_USERNAME = "username"
    @JvmField
    var API_QUOTA_KEY_PASSWORD = "password"
}
