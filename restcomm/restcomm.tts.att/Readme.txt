Restcomm plugin for AT&T TTS server
-----------------------------------

Author: George Vagenas
Date:   January 24, 2014

Restcomm plugin for AT&T TTS server. The plugin is using the NaturalVoice Java wrapper to communicate with the AT&T TTS server (the Java wrapper is using Java ProcessBuilder to execute TTSClientFile)

AT&T Version
============

	1. AT&T TTS Server: att_naturalvoices_v5.1_server_live
	2. AT&T Java Wrapper: NV51-Java


Prerequisite
==============

	1. AT&T Server needs to be configured with the appropriate language/voice fonts and running.
	2. The plugin will need to use TTSClientFile application ($ATT_TTS_DIR/bin/TTSClientFile) so if TTS server is running on a different server, 
	   copy this file to a local directory and configure Restcomm plugin accordingly

How to configure and run AT&T TTS server
========================================

If OS architecture is amd64, copy the lib64/ folder as lib/ and use the bin64/TTSServer executable.

For example:
	./TTSServer -c 7000 -r /home/user/att_naturalvoices_v5.1_server_live/data/ -x mike8 -v3

In order to use specific voice fonts, edit the data/tts.cfg and remove one # in front of the voice font you wish to use.
For example to use "mike8" and "crystal8" change :

		## US ENGLISH FONTS
		##include "en_us/mike8/mike8.cfg"
		##include "en_us/mike16/mike16.cfg"
		##include "en_us/crystal8/crystal8.cfg"
		##include "en_us/crystal16/crystal16.cfg"
		##include "en_us/rich8/rich8.cfg"
		##include "en_us/rich16/rich16.cfg"
		##include "en_us/ray8/ray8.cfg"
		##include "en_us/ray16/ray16.cfg"
		##include "en_us/claire8/claire8.cfg"
		##include "en_us/claire16/claire16.cfg"
		##include "en_us/julia8/julia8.cfg"
		##include "en_us/julia16/julia16.cfg"
		##include "en_us/lauren8/lauren8.cfg"
		##include "en_us/lauren16/lauren16.cfg"
		##include "en_us/mel8/mel8.cfg"
		##include "en_us/mel16/mel16.cfg"

to:

		## US ENGLISH FONTS
		#include "en_us/mike8/mike8.cfg"		<--- Notice that there is only 1 # before the include
		##include "en_us/mike16/mike16.cfg"
		#include "en_us/crystal8/crystal8.cfg"		<--- Notice that there is only 1 # before the include
		##include "en_us/crystal16/crystal16.cfg"
		##include "en_us/rich8/rich8.cfg"
		##include "en_us/rich16/rich16.cfg"
		##include "en_us/ray8/ray8.cfg"
		##include "en_us/ray16/ray16.cfg"
		##include "en_us/claire8/claire8.cfg"
		##include "en_us/claire16/claire16.cfg"
		##include "en_us/julia8/julia8.cfg"
		##include "en_us/julia16/julia16.cfg"
		##include "en_us/lauren8/lauren8.cfg"
		##include "en_us/lauren16/lauren16.cfg"
		##include "en_us/mel8/mel8.cfg"
		##include "en_us/mel16/mel16.cfg"


Supported languages
===================

Following a list of the supported languages:

	1. US English (language code: en)
		1.1 Male voice "mike8"
		1.2 Female voice "crystal8"
	2. UK English (language code: en-uk)
		2.1 Male voice "charles8"
		2.2 Female voice "audrey8"
	3. Spanish (language code: es)
		3.1 Male voice "alberto8"
		3.2 Female voice "rosa8"
	4. French (language code: fr)
		4.1 Male voice "alain8"
		4.2 Female voice "juliette8"
	5. French Canadian (language code: fr-ca)
		5.1 Male voice "arnaud8"
	6. German (language code: de)
		6.1 Male voice "reiner8"
		6.2 Female voice "klara8"		
	7. Italina (language code: it)
		7.1 Male voice "giovanni8"
		7.2 Female voice "francesca8"
	8. Brazilian Portuguese (language code: )
		8.1 Male voice "tiago8"
		8.2 Female voice "marina8"


Configuration
=============

To configure the plugin, find the AT&T TTS configuration section at the standard Restcomm configuration file ($RESTCOMM/WEB-INF/conf/restcomm.xml).

	<speech-synthesizer
		class="org.mobicents.servlet.restcomm.tts.AttSpeechSynthesizer">
		<host>127.0.0.1</host>
		<port>7000</port>
		<tts-client-directory></tts-client-directory>
		<verbose-output>false</verbose-output>
		<speakers>
			<english>
				<female>crystal8</female>
				<male>mike8</male>
			</english>
			<english-uk>
				<female>audrey8</female>
				<male>charles8</male>
			</english-uk>
			<spanish>
				<female>rosa8</female>
				<male>alberto8</male>
			</spanish>
			<french>
				<female>juliette8</female>
				<male>alain8</male>
			</french>
			<canadian-french>
				<male>arnaud8</male>
			</canadian-french>
			<german>
				<female>klara8</female>
				<male>reiner8</male>
			</german>
			<italian>
				<female>francesca8</female>
				<male>giovanni8</male>
			</italian>
			<brazilian-portuguese>
				<female>marina8</female>
				<male>tiago8</male>
			</brazilian-portuguese>
		</speakers>
	</speech-synthesizer>

User must provide the following:
	1. host: The host ip address where the AT&T TTS server is running
	2. port: The port where the AT&T TTS server is bind to
	3. (*) tts-client-directory: The directory where the TTSClientFile application can be found
	4. verbose-output: Whether or not the AT&T Java Player will output to standard error

(*) If AT&T TTS server is not running at the same server as Restcomm, you need to copy the TTSClientFile to a local directory.

Example configuration:

	<speech-synthesizer
		class="org.mobicents.servlet.restcomm.tts.AttSpeechSynthesizer">
		<host>127.0.0.1</host>
		<port>7000</port>
		<tts-client-directory>/home/user/att_naturalvoices_v5.1_server_live</tts-client-directory>
		<verbose-output>true</verbose-output>
		<speakers>
			<english>
				<female>crystal8</female>
				<male>mike8</male>
			</english>
			<english-uk>
				<female>audrey8</female>
				<male>charles8</male>
			</english-uk>
			<spanish>
				<female>rosa8</female>
				<male>alberto8</male>
			</spanish>
			<french>
				<female>juliette8</female>
				<male>alain8</male>
			</french>
			<canadian-french>
				<female>arnaud8</female>
				<male>arnaud8</male>
			</canadian-french>
			<german>
				<female>klara8</female>
				<male>reiner8</male>
			</german>
			<italian>
				<female>francesca8</female>
				<male>giovanni8</male>
			</italian>
			<brazilian-portuguese>
				<female>marina8</female>
				<male>tiago8</male>
			</brazilian-portuguese>
		</speakers>
	</speech-synthesizer>


