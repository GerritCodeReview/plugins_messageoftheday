include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

TEST_DEPS = GERRIT_PLUGIN_API + GERRIT_TESTS + [
  ':messageoftheday__plugin',
]

gerrit_plugin(
  name = 'messageoftheday',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: messageoftheday',
    'Gerrit-Module: com.googlesource.gerrit.plugins.messageoftheday.Module',
  ]
)

java_sources(
  name = 'messageoftheday-sources',
  srcs = SOURCES + RESOURCES,
)

java_library(
  name = 'classpath',
  deps = TEST_DEPS,
)

java_test(
  name = 'messageoftheday_tests',
  srcs = glob(['src/test/java/**/*.java']),
  resources = glob(['src/test/resources/**/']),
  labels = ['messageoftheday'],
  deps = TEST_DEPS,
)
