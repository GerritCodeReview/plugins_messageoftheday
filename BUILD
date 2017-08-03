load("//tools/bzl:plugin.bzl", "gerrit_plugin")

gerrit_plugin(
    name = "messageoftheday",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: messageoftheday",
        "Gerrit-Module: com.googlesource.gerrit.plugins.messageoftheday.Module",
        "Implementation-Title: Plugin messageoftheday",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/messageoftheday",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
