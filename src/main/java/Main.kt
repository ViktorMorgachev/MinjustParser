import kotlinx.coroutines.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.lang.Exception
import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

fun main(args: Array<String>) {
    args.firstOrNull()?.let {
        city = it
    }
    parse(parseUrl)
}

fun parse(url: String) = runBlocking {
    val ids = getIds(url)
    val coroutines = mutableListOf<Job>()
    val results = Collections.synchronizedList(listOf<Data>())
    ids.forEach { pageID ->
        Logger.log("Trying parse ID: $pageID")
        coroutines.add(launch {
            parsePage(pageID)?.let {
                Logger.log("Data from page: $it")
                results.add(it)
            }
        })
    }
    coroutines.forEach {
        it.join()
    }

    if (results.isNotEmpty()) {
        val currentDateTime = SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().time)
        val cvsfile = "${city}_Saves_$currentDateTime.csv"
        val writer = Files.newBufferedWriter(Paths.get(cvsfile))
        val csvPrinter = CSVPrinter(
            writer, CSVFormat.DEFAULT
                .withHeader("Company", "PhoneNumber", "Owner", "DateOfRegistering", "City")
        )
        results.forEach {
            val (company, phone, owner, dateOfRegistering) = it
            csvPrinter.printRecord(company, phone, owner, dateOfRegistering, city)
        }
        csvPrinter.flush()
        csvPrinter.close()
    }
    Logger.log("It's Done")
}

fun getIds(url: String): List<String> {
    try {
        val doc: Document = Jsoup.connect(url).get()
        Logger.log("Loaded host: ${doc.title()}")
    } catch (e: ConnectException) {
        Logger.log("Host $parseUrl is unavailable")
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return emptyList()
}

suspend fun parsePage(pageID: String): Data? {
    val searchPage =
        "$parseUrl/SearchAction.seam?firstResult=17825&city=$city&founder=&chief=&house=&room=&okpo=&number=&baseBusiness=&fullnameRu=&street=&district=&tin=&logic=and&category=5&region=&cid=$pageID"
    val doc: Document = Jsoup.connect("searchPage").get()
    Logger.log("Loaded host: ${doc.title()}")
    // Some code for parsing
    return null
}