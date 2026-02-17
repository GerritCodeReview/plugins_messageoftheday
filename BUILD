load("@rules_java//java:defs.bzl", "java_library")
load("//tools/bzl:js.bzl", "gerrit_js_bundle")
load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
    "gerrit_plugin",
)

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

junit_tests(
    name = "messageoftheday_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["messageoftheday"],
    deps = [":messageoftheday__plugin_test_deps"],
)

java_library(
    name = "messageoftheday__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [":messageoftheday__plugin"],
)
