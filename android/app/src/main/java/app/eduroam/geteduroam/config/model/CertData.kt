package app.eduroam.geteduroam.config.model

import com.squareup.moshi.JsonClass
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root
import org.simpleframework.xml.Text

@Root(name = "CA")
@JsonClass(generateAdapter = true)
class CertData {

    @field:Text
    var value: String? = null

    @field:Attribute
    var format: String? = null

    @field:Attribute
    var encoding: String? = null
}