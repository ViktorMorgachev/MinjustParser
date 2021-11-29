import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.net.ConnectException
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

fun main(args: Array<String>) {
    args.firstOrNull()?.let {
        city = it
    }
    parse()
}

fun parse() = runBlocking {
    val results = Collections.synchronizedList(listOf<Data>())
    val parseDocumentJobs = mutableListOf<Job>()
    getIds().onCompletion {
        Logger.log("OnCompletion")
        saveResults(results)
    }.collect { documentID ->
        Logger.log("Trying parse ID: $documentID")
        parseDocumentJobs.add(launch {
            parseDocument(documentID)?.let {
                Logger.log("Data from page: $it")
                results.add(it)
            }
        })
        parseDocumentJobs.forEach {
            it.join()
        }
    }
    Logger.log("It's Done")
}

fun saveResults(results: List<Data>) {
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
}

fun getIdsByPage(firstResult: Int): List<String> {
    val ids = mutableListOf<String>()
    val doc: Document = Jsoup.connect(getParsePageUrl(firstResult = firstResult)).get()
    val form = doc.allElements.filter { it.tagName() == "form" && it.getElementById("searchActionForm") != null }.first() ?: return emptyList()
    val table: Element = form.select("tbody").firstOrNull() ?: return emptyList()
    val rows: Elements = table.select("tr")
    rows.forEach { column ->
        ids.add(column.select("td")[0].text())
    }
    return ids
}

suspend fun getIds() = flow {
    coroutineScope {
        try {
            for (pages in 1..Int.MAX_VALUE step 25) {
                val ids = getIdsByPage(pages)
                if (ids.isEmpty()) break
                emitAll(ids.asFlow())
            }
        } catch (e: ConnectException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}


fun parseDocument(documentID: String): Data? {
    val searchPage = getParseDocumentUrl(documentID)
    val doc: Document = Jsoup.connect(searchPage).get()
    Logger.log("Loaded host: ${doc.title()}")
    // Some code for parsing
    return null
}

fun getParsePageUrl(cityName: String = city, firstResult: Int): String {
    return "https://register.minjust.gov.kg/register/SearchAction.seam?firstResult=$firstResult&city=$cityName&logic=and&cid=14340"
}

fun getParseDocumentUrl(documentID: String): String {
    return "https://register.minjust.gov.kg/register/Public.seam?publicId=$documentID"
}