RestComm build
========
Since RestComm is lead by [TeleStax](http://www.telestax.com/), Inc. and developed collaboratively by a community of individual and enterprise contributors it does not prevent You to try 
the community version and build Yourself.
Before the build itself you are required to download the dependent deliverables. 
Use following commands: 
```
ant -f ddd-s3-box.xml box 
```
to download the dependent deliverables for Restcomm Connect, which are
* [Sip Servlets](https://github.com/RestComm/sip-servlets)
* [Media Server](https://github.com/RestComm/mediaserver)
* [Olympus](https://github.com/RestComm/olympus)
* [Visual Designer](https://github.com/RestComm/visual-designer)

All mentioned above, must be built and available prior to your build of RestComm Connect, so please make sure to get familiar with 
those projects too.
Inside file 
```
ddd-versions.properties
```
you will find the configuration of the deliverables, which are going to be downloaded. In case you want to use default and predefined versions
as you cloned from the repository, do not change anything. For advance usage and testing a different versions, it is your responsibility to 
define proper versions.
For those able to access the [TeleStax](http://www.telestax.com/), Inc. infrastructure and AWS S3, take advantage of using the 
```
ant -f ddd-s3-box.xml s3
```

To build the Restcomm Connect project itself please use:
```
ant -f build.xml
```
Optionaly, if you want to provide your own settings.xml for your inner maven build use:

```
ant -f build.xml -Dmvn.arg='-s ${location of your settings.xml file}'
```
Other possible targets to use are:
```
deploy, release-test
```

Check the build.xml for more details and specifically properties section
```
  \<property name="release.build.goals" value="clean install -Dmaven.test.skip=true"/\>
  \<property name="release.build.test.goals" value="clean install -Dmaven.test.failure.ignore=true"/\>
  \<property name="release.ts.deploy.goals" value="clean deploy"/\>

```

