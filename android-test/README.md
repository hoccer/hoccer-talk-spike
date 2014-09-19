# android-test

There is now support for Android UI testing.
Therefore we added a new maven project called [android-test](../blob/develop/android-test)

## Running the tests

```
mvn clean install -pl android-test
```
## Adding new tests

Take a look at [SingleProfileActivityTest.java](../blob/develop/android-test/src/main/java/com/hoccer/xo/android/activity/SingleProfileActivityTest.java). This can be used as a starting point for your own tests.

1. The name of your test class should be *Test
1. The name of your test methodes should be test*˘


## IntelliJ Run Configuration

Run > Edit Configurations > + > Android Tests > Enter name and choose module: hoccer-talk-android-test > Choose target device > OK

