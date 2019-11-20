package com.fijimf.deepfij.scraping.util

import com.fijimf.deepfij.scraping.BuildInfo

object Banner {
  val banner =List(s"""   ____  ____  ____  ____
                      |  (    \\(  __)(  __)(  _ \\
                      |   ) D ( ) _)  ) _)  ) __/
                      |  (____/(____)(____)(__)
                      |   ____  __    __
                      |  (  __)(  ) _(  )
                      |   ) _)  )( / \\) \\
                      |  (__)  (__)\\____/
                      |  \u001b[1m${BuildInfo.name} v${BuildInfo.version}\u001b[0m
                      |  \u001b[1m${BuildInfo.builtAtString}\u001b[0m
                      |""".stripMargin)

}
