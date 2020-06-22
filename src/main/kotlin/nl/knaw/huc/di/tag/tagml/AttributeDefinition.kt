package nl.knaw.huc.di.tag.tagml

data class AttributeDefinition(
        val name: String,
        val description: String,
        val dataType: String,
        val ref: String = ""
)
