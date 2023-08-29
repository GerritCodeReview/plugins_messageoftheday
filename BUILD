load("//tools/bzl:plugin.bzl", "gerrit_plugin")
load("//tools/bzl:js.bzl", "gerrit_js_bundle")

gerrit_plugin(
    name = "messageoftheday",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: messageoftheday",
        "Gerrit-Module: com.googlesource.gerrit.plugins.messageoftheday.Module",
        "Gerrit-HttpModule: com.googlesource.gerrit.plugins.messageoftheday.HttpModule",
        "Implementation-Title: Plugin messageoftheday",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/messageoftheday",
    ],
    resource_jars = [":gr-messageoftheday"],
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_js_bundle(
    name = "gr-messageoftheday",
    srcs = glob(["gr-messageoftheday/*.js"]),
    entry_point = "gr-messageoftheday/plugin.js",
)
