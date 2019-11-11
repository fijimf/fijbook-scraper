package com.fijimf.deepfij.scraping.model
import java.io.{Reader, StringReader}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import cats.implicits._
import com.fijimf.deepfij.schedule.model.UpdateCandidate
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import org.xml.sax.InputSource

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}
import scala.xml.parsing.NoBindingFactoryAdapter
import scala.xml.{Node, NodeSeq}

case class Web1NcaaScraper(season:Int) extends TeamBasedScrapingModel {
  override val modelName: String = "web1ncaa"

  override def keys: List[String] = Web1NcaaKey.codeToKey.keys.map(_.toString).toList

  override def urlFromKey(k: String): String = s"http://web1.ncaa.org/stats/exec/records?doWhat=display&useData=CAREER&sportCode=MBB&academicYear=$season&orgId=$k&division=1&playerId=-100"

  override def scrape(key:String, data: String): List[UpdateCandidate] = Web1NcaaParser.parseGames(key, data)

  override def modelKey(k: String): String = k

  override val rateLimit: Option[(Long, FiniteDuration)] = Some(1L, 1.second)
}

object Web1NcaaParser{

  def extractUpdates(key:String, root: Node): List[UpdateCandidate] = {
    val rows: NodeSeq = extractGameRows(root)
    rows.flatMap(extractGame(key.toInt,_)).toList
  }

  def extractGameRows(root: Node): NodeSeq = {
    (root \ "body" \ "table" \\ "tr")
      .filter(n=>
        checkAttributeEquals(n, "class", "text") && checkTdForPercent(n)
      )
  }

  def parseGames(key:String, data: String): List[UpdateCandidate] = {
    loadFromString(data) match {
      case Success(root)=>
        extractUpdates(key, root)
      case Failure(_)=>List.empty[UpdateCandidate]
    }
  }

  def loadFromReader(r: Reader): Try[Node] = {
    Try {
      new NoBindingFactoryAdapter().loadXML(new InputSource(r), new SAXFactoryImpl().newSAXParser())
    }
  }

  def loadFromString(s: String): Try[Node] =  loadFromReader(new StringReader(s))


  def extractGame(teamId:Int,row:Node):Option[UpdateCandidate] = {
    val cells: NodeSeq = row \ "td"
    if (cells.headOption.exists(_.text.contains("%"))){
      cells.toList match {
        case oppNode::dateNode::scoreNode::oppScoreNode::homeAwayNode::_::otNode::_::Nil=>
          val oppId: Int = oppNodeToCode(oppNode)
          val dateTime: LocalDateTime = LocalDate.parse(dateNode.text.trim, DateTimeFormatter.ofPattern("MM/dd/yyyy")).atStartOfDay()
          val homeAway:String = homeAwayNode.text.trim
          val score: Int =scoreNode.text.trim.toInt
          val oppScore: Int =oppScoreNode.text.trim.toInt
          val otString: String =otNode.text.trim

          for {
            team<-Web1NcaaKey.codeToKey.get(teamId)
            opponent<-Web1NcaaKey.codeToKey.get(oppId)
            uc<-buildUpdateCandidate(teamId, oppId, dateTime, homeAway, score, oppScore, otString, team, opponent)
          } yield {
            uc
          }
        case _ =>None
      }
    } else {
      None
    }
  }

  private def buildUpdateCandidate(teamId: Int, oppId: Int, dateTime: LocalDateTime, homeAway: String, score: Int, oppScore: Int, otString: String, team: String, opponent: String): Option[UpdateCandidate] = {
    val (ht, hs, at, as) = if (homeAway.equalsIgnoreCase("home")) {
      (team, score, opponent, oppScore)
    } else if (homeAway.equalsIgnoreCase("away")) {
      (opponent, oppScore, team, score)
    } else {
      if (teamId < oppId) {
        (team, score, opponent, oppScore)
      } else {
        (opponent, oppScore, team, score)
      }
    }
    if (team === ht)
      Some(UpdateCandidate(
        dateTime,
        ht,
        at,
        None,
        Some(homeAway.equalsIgnoreCase("neutral")),
        Some(hs),
        Some(as),
        Some(otStringToNumPeriods(otString))
      )) else None
  }

  def oppNodeToCode(oppNode:Node): Int ={
    val extractor: Regex = """javascript:showTeamResults\((\d+)\);""".r
    (for {
      a <- oppNode \ "a"
      hr <- a.attribute("href")
      hr0 <- hr.headOption
    }yield {
      hr0.text.trim match {
        case extractor(t) => t.toInt
        case _ => -1
      }
    }).headOption.getOrElse(-1)
  }

    def otStringToNumPeriods(s:String): Int ={
    s.toLowerCase.trim match {
      case "-"=>2
      case "1 ot"=>3
      case "2 ot"=>4
      case "3 ot"=>5
      case "4 ot"=>6
      case "5 ot"=>7
      case "6 ot"=>8
      case _ =>2
    }
  }

  private def checkAttributeEquals(node:Node, attribute: String, value:String):Boolean = {
    node.attributes.exists(_.value.text === value)
  }

  def checkTdForPercent(node:Node):Boolean ={
    val option: Option[Node] = (node \ "td").headOption
    option.exists(_.text.contains("%"))
  }
}

object Web1NcaaKey {
  val keyToCode: Map[String, Int] = Map(
    "abilene-christian" -> 2,
    "air-force" -> 721,
    "akron" -> 5,
    "alabama" -> 8,
    "alabama-am" -> 6,
    "alabama-st" -> 7,
    "albany-ny" -> 14,
    "alcorn" -> 17,
    "am-corpus-chris" -> 26172,
    "american" -> 23,
    "appalachian-st" -> 27,
    "arizona" -> 29,
    "arizona-st" -> 28,
    "ark-pine-bluff" -> 2678,
    "arkansas" -> 31,
    "arkansas-st" -> 30,
    "army" -> 725,
    "auburn" -> 37,
    "austin-peay" -> 43,
    "bakersfield" -> 94,
    "ball-st" -> 47,
    "baylor" -> 51,
    "belmont" -> 14927,
    "bethune-cookman" -> 61,
    "binghamton" -> 62,
    "boise-st" -> 66,
    "boston-college" -> 67,
    "boston-u" -> 68,
    "bowling-green" -> 71,
    "bradley" -> 72,
    "brown" -> 80,
    "bryant" -> 81,
    "bucknell" -> 83,
    "buffalo" -> 86,
    "butler" -> 87,
    "byu" -> 77,
    "cal-poly" -> 90,
    "cal-st-fullerton" -> 97,
    "cal-st-northridge" -> 101,
    "california" -> 107,
    "campbell" -> 115,
    "canisius" -> 116,
    "central-ark" -> 1004,
    "central-conn-st" -> 127,
    "central-mich" -> 129,
    "charleston-so" -> 48,
    "charlotte" -> 458,
    "chattanooga" -> 693,
    "chicago-st" -> 136,
    "cincinnati" -> 140,
    "citadel" -> 141,
    "clemson" -> 147,
    "cleveland-st" -> 148,
    "coastal-caro" -> 149,
    "col-of-charleston" -> 1014,
    "colgate" -> 153,
    "colorado" -> 157,
    "colorado-st" -> 156,
    "columbia" -> 158,
    "coppin-st" -> 165,
    "cornell" -> 167,
    "creighton" -> 169,
    "dartmouth" -> 172,
    "davidson" -> 173,
    "dayton" -> 175,
    "delaware" -> 180,
    "delaware-st" -> 178,
    "denver" -> 183,
    "depaul" -> 176,
    "detroit" -> 184,
    "drake" -> 189,
    "drexel" -> 191,
    "duke" -> 193,
    "duquesne" -> 194,
    "east-carolina" -> 196,
    "east-tenn-st" -> 198,
    "eastern-ill" -> 201,
    "eastern-ky" -> 202,
    "eastern-mich" -> 204,
    "eastern-wash" -> 207,
    "elon" -> 1068,
    "evansville" -> 219,
    "fairfield" -> 220,
    "fairleigh-dickinson" -> 222,
    "fgcu" -> 28755,
    "fiu" -> 231,
    "fla-atlantic" -> 229,
    "florida" -> 235,
    "florida-am" -> 228,
    "florida-st" -> 234,
    "fordham" -> 236,
    "fresno-st" -> 96,
    "furman" -> 244,
    "ga-southern" -> 253,
    "gardner-webb" -> 1092,
    "george-mason" -> 248,
    "george-washington" -> 249,
    "georgetown" -> 251,
    "georgia" -> 257,
    "georgia-st" -> 254,
    "georgia-tech" -> 255,
    "gonzaga" -> 260,
    "grambling" -> 261,
    "grand-canyon" -> 1104,
    "green-bay" -> 794,
    "hampton" -> 270,
    "hartford" -> 272,
    "harvard" -> 275,
    "hawaii" -> 277,
    "high-point" -> 19651,
    "hofstra" -> 283,
    "holy-cross" -> 285,
    "houston" -> 288,
    "houston-baptist" -> 287,
    "howard" -> 290,
    "idaho" -> 295,
    "idaho-st" -> 294,
    "ill-chicago" -> 302,
    "illinois" -> 301,
    "illinois-st" -> 299,
    "incarnate-word" -> 2743,
    "indiana" -> 306,
    "indiana-st" -> 305,
    "iona" -> 310,
    "iowa" -> 312,
    "iowa-st" -> 311,
    "ipfw" -> 308,
    "iupui" -> 2699,
    "jackson-st" -> 314,
    "jacksonville" -> 316,
    "jacksonville-st" -> 315,
    "james-madison" -> 317,
    "kansas" -> 328,
    "kansas-st" -> 327,
    "kennesaw-st" -> 1157,
    "kent-st" -> 331,
    "kentucky" -> 334,
    "la-lafayette" -> 671,
    "la-monroe" -> 498,
    "la-salle" -> 340,
    "lafayette" -> 342,
    "lamar" -> 346,
    "lehigh" -> 352,
    "liberty" -> 355,
    "lipscomb" -> 28600,
    "long-beach-st" -> 99,
    "long-island" -> 361,
    "longwood" -> 363,
    "louisiana-tech" -> 366,
    "louisville" -> 367,
    "loyola-il" -> 371,
    "loyola-maryland" -> 369,
    "loyola-marymount" -> 370,
    "lsu" -> 365,
    "maine" -> 380,
    "manhattan" -> 381,
    "marist" -> 386,
    "marquette" -> 387,
    "marshall" -> 388,
    "maryland" -> 392,
    "mass-lowell" -> 368,
    "massachusetts" -> 400,
    "mcneese-st" -> 402,
    "memphis" -> 404,
    "mercer" -> 406,
    "miami-fl" -> 415,
    "miami-oh" -> 414,
    "michigan" -> 418,
    "michigan-st" -> 416,
    "middle-tenn" -> 419,
    "milwaukee" -> 797,
    "minnesota" -> 428,
    "mississippi-st" -> 430,
    "mississippi-val" -> 432,
    "missouri" -> 434,
    "missouri-st" -> 435,
    "monmouth" -> 439,
    "montana" -> 441,
    "montana-st" -> 440,
    "morehead-st" -> 444,
    "morgan-st" -> 446,
    "mt-st-marys" -> 450,
    "murray-st" -> 454,
    "navy" -> 726,
    "nc-at" -> 488,
    "nc-central" -> 489,
    "neb-omaha" -> 464,
    "nebraska" -> 463,
    "nevada" -> 466,
    "new-hampshire" -> 469,
    "new-mexico" -> 473,
    "new-mexico-st" -> 472,
    "new-orleans" -> 474,
    "niagara" -> 482,
    "nicholls-st" -> 483,
    "njit" -> 471,
    "norfolk-st" -> 485,
    "north-carolina" -> 457,
    "north-carolina-st" -> 490,
    "north-dakota" -> 494,
    "north-dakota-st" -> 493,
    "north-florida" -> 2711,
    "north-texas" -> 497,
    "northeastern" -> 500,
    "northern-ariz" -> 501,
    "northern-colo" -> 502,
    "northern-ill" -> 503,
    "northern-ky" -> 505,
    "northwestern" -> 509,
    "northwestern-st" -> 508,
    "notre-dame" -> 513,
    "oakland" -> 514,
    "ohio" -> 519,
    "ohio-st" -> 518,
    "oklahoma" -> 522,
    "oklahoma-st" -> 521,
    "old-dominion" -> 523,
    "ole-miss" -> 433,
    "oral-roberts" -> 527,
    "oregon" -> 529,
    "oregon-st" -> 528,
    "pacific" -> 534,
    "penn" -> 540,
    "penn-st" -> 539,
    "pepperdine" -> 541,
    "pittsburgh" -> 545,
    "portland" -> 551,
    "portland-st" -> 550,
    "prairie-view" -> 553,
    "presbyterian" -> 1320,
    "princeton" -> 554,
    "providence" -> 556,
    "purdue" -> 559,
    "quinnipiac" -> 562,
    "radford" -> 563,
    "rhode-island" -> 572,
    "rice" -> 574,
    "richmond" -> 575,
    "rider" -> 576,
    "robert-morris" -> 579,
    "rutgers" -> 587,
    "sacramento-st" -> 102,
    "sacred-heart" -> 590,
    "saint-josephs" -> 606,
    "saint-louis" -> 609,
    "sam-houston-st" -> 624,
    "samford" -> 625,
    "san-diego" -> 627,
    "san-diego-st" -> 626,
    "san-francisco" -> 629,
    "san-jose-st" -> 630,
    "santa-clara" -> 631,
    "savannah-st" -> 632,
    "seattle" -> 1356,
    "seton-hall" -> 635,
    "siena" -> 639,
    "siu-edwardsville" -> 660,
    "smu" -> 663,
    "south-ala" -> 646,
    "south-carolina" -> 648,
    "south-carolina-st" -> 647,
    "south-dakota" -> 650,
    "south-dakota-st" -> 649,
    "south-fla" -> 651,
    "southeast-mo-st" -> 654,
    "southeastern-la" -> 655,
    "southern-california" -> 657,
    "southern-ill" -> 659,
    "southern-miss" -> 664,
    "southern-u" -> 665,
    "southern-utah" -> 667,
    "st-bonaventure" -> 596,
    "st-francis-ny" -> 599,
    "st-francis-pa" -> 600,
    "st-johns-ny" -> 603,
    "st-marys-ca" -> 610,
    "st-peters" -> 617,
    "stanford" -> 674,
    "stephen-f-austin" -> 676,
    "stetson" -> 678,
    "stony-brook" -> 683,
    "syracuse" -> 688,
    "tcu" -> 698,
    "temple" -> 690,
    "tennessee" -> 694,
    "tennessee-st" -> 691,
    "tennessee-tech" -> 692,
    "texas" -> 703,
    "texas-am" -> 697,
    "texas-arlington" -> 702,
    "texas-southern" -> 699,
    "texas-st" -> 670,
    "texas-tech" -> 700,
    "toledo" -> 709,
    "towson" -> 711,
    "troy" -> 716,
    "tulane" -> 718,
    "tulsa" -> 719,
    "uab" -> 9,
    "ualr" -> 32,
    "uc-davis" -> 108,
    "uc-irvine" -> 109,
    "uc-riverside" -> 111,
    "uc-santa-barbara" -> 104,
    "ucf" -> 128,
    "ucla" -> 110,
    "uconn" -> 164,
    "umbc" -> 391,
    "umes" -> 393,
    "umkc" -> 2707,
    "unc-asheville" -> 456,
    "unc-greensboro" -> 459,
    "unc-wilmington" -> 460,
    "uni" -> 504,
    "unlv" -> 465,
    "usc-upstate" -> 10411,
    "ut-martin" -> 695,
    "utah" -> 732,
    "utah-st" -> 731,
    "utah-valley" -> 30024,
    "utep" -> 704,
    "utrgv" -> 536,
    "utsa" -> 706,
    "valparaiso" -> 735,
    "vanderbilt" -> 736,
    "vcu" -> 740,
    "vermont" -> 738,
    "villanova" -> 739,
    "virginia" -> 746,
    "virginia-tech" -> 742,
    "vmi" -> 741,
    "wagner" -> 748,
    "wake-forest" -> 749,
    "washington" -> 756,
    "washington-st" -> 754,
    "weber-st" -> 758,
    "west-virginia" -> 768,
    "western-caro" -> 769,
    "western-ill" -> 771,
    "western-ky" -> 772,
    "western-mich" -> 774,
    "wichita-st" -> 782,
    "william-mary" -> 786,
    "winthrop" -> 792,
    "wisconsin" -> 796,
    "wofford" -> 2915,
    "wright-st" -> 810,
    "wyoming" -> 811,
    "xavier" -> 812,
    "yale" -> 813,
    "youngstown-st" -> 817
  )

  val codeToKey: Map[Int, String] = keyToCode.map { case (k: String, v: Int) => v -> k }
}