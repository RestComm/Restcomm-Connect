# How to Debug TestSuite when using Arquillian Manager container

To debug a managed container we need to use remote debugging.
Check here: http://arquillian.org/guides/getting_started_rinse_and_repeat/#debug_a_managed_server

1. Edit `arquillian.xml` to include the following:

`<property name="javaVmArguments">-agentlib:jdwp=transport=dt_socket,address=8787,server=y,suspend=y -Dorg.jboss.server.bootstrap.maxThreads=1</property>`

Using the above, when the test case starts it will suspend waiting for the remote debugger to connect

2. Using IDE settings, prepare remote debugging configuration that points to 127.0.0.1:8787

3. Start test case, you will notice it suspend waiting for remote debugger

4. Start remote debugger

## Important note

Remote debugging property should always be removed, otherwise CI and CD jobs will fail to run automatically testsuite
