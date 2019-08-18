class Config {
    static applicationId = "com.myxh.yellchat"
    static appName = "YellChat"

    static compileSdkVersion = 28
    static minSdkVersion = 19
    static targetSdkVersion = 28
    static versionCode = 1
    static versionName = "1.0"

    static kotlin_version = '1.3.41'

    //app 运行模块配置, git 提交只包含 app
    static appConfig = ['app', 'user']
    //pkg 依赖功能包配置
    static pkgConfig = ['user']
    static depConfig = [
            plugin : [
                    gradle : new DepConfig("com.android.tools.build:gradle:3.4.1"),
                    kotlin : new DepConfig("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
            ],

            core : [
                    user : [
                            app     : new DepConfig(":core:user:app"),
                            pkg     : new DepConfig(true, ":core:user:pkg", "远程仓库地址", true),
                            export  : new DepConfig(true, ":core:user:export", "远程仓库地址")
                    ],
                    chat : [
                            app     : new DepConfig(":core:chat:app"),
                            pkg     : new DepConfig(true, ":core:chat:pkg", "远程仓库地址", true),
                            export  : new DepConfig(true, ":core:chat:export", "远程仓库地址")
                    ],
                    common : new DepConfig(":core:common")
            ],

            base : [
                    arch        : new DepConfig(":base:arch"),
                    net         : new DepConfig(":base:net"),
                    util        : new DepConfig(":base:util"),
                    widget      : new DepConfig(":base:widget"),
                    livedatabus : new DepConfig(":base:livedatabus")
            ]
    ]
}