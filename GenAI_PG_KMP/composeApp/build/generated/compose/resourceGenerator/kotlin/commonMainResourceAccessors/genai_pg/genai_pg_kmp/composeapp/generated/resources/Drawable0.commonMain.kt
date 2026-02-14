@file:OptIn(InternalResourceApi::class)

package genai_pg.genai_pg_kmp.composeapp.generated.resources

import kotlin.OptIn
import kotlin.String
import kotlin.collections.MutableMap
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.InternalResourceApi
import org.jetbrains.compose.resources.ResourceContentHash
import org.jetbrains.compose.resources.ResourceItem

private const val MD: String =
    "composeResources/genai_pg.genai_pg_kmp.composeapp.generated.resources/"

@delegate:ResourceContentHash(-31_121_552)
public val Res.drawable.rounded_downloading_24: DrawableResource by lazy {
      DrawableResource("drawable:rounded_downloading_24", setOf(
        ResourceItem(setOf(), "${MD}drawable/rounded_downloading_24.xml", -1, -1),
      ))
    }

@delegate:ResourceContentHash(-623_389_464)
public val Res.drawable.rounded_file_open_24: DrawableResource by lazy {
      DrawableResource("drawable:rounded_file_open_24", setOf(
        ResourceItem(setOf(), "${MD}drawable/rounded_file_open_24.xml", -1, -1),
      ))
    }

@InternalResourceApi
internal fun _collectCommonMainDrawable0Resources(map: MutableMap<String, DrawableResource>) {
  map.put("rounded_downloading_24", Res.drawable.rounded_downloading_24)
  map.put("rounded_file_open_24", Res.drawable.rounded_file_open_24)
}
