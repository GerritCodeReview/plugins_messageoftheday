load("//tools/bzl:plugin.bzl", "gerrit_plugin")

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
    resource_jars = [":gr-messageoftheday-static"],
    resources = glob(["src/main/resources/**/*"]),
)

genrule2(
    name = "gr-messageoftheday-static",
    srcs = [":gr-messageoftheday"],
    outs = ["gr-messageoftheday-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -r $(locations :gr-messageoftheday) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "gr-messageoftheday",
    app = "gr-messageoftheday-bundle.js",
    plugin_name = "gr-messageoftheday",
)

rollup_bundle(
    name = "gr-messageoftheday-bundle",
    srcs = glob(["gr-messageoftheday/*.js"]),
    entry_point = "gr-messageoftheday/plugin.js",
    format = "iife",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
)
