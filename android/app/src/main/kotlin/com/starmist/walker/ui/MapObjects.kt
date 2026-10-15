package com.starmist.walker.ui

/** A painted object on the island map: [id] names the drawable `art_obj_<id>`; [widthKm] is how wide it is drawn on the ground. */
internal data class MapObject(val id: String, val lat: Double, val lon: Double, val widthKm: Double, val landmark: Boolean = false)

/** Local colour that is not a sight (Gemini sheet O4); positions are illustrative but placed where the thing really is. */
internal val LOCAL_OBJECTS = listOf(
    MapObject("local_0", 24.935, 121.715, 8.0),
    MapObject("local_1", 25.137, 121.505, 7.0),
    MapObject("local_2", 25.168, 121.435, 7.0),
    MapObject("local_3", 25.05, 121.20, 8.0),
    MapObject("local_4", 24.70, 121.78, 8.0),
    MapObject("local_5", 23.50, 121.37, 8.0),
    MapObject("local_6", 23.15, 121.23, 8.0),
    MapObject("local_7", 22.45, 120.52, 8.0),
    MapObject("local_8", 21.96, 120.80, 8.0),
    MapObject("local_9", 23.15, 120.08, 8.0),
    MapObject("local_10", 23.45, 120.14, 8.0),
    MapObject("local_11", 23.75, 120.20, 8.0),
    MapObject("local_12", 23.97, 120.35, 8.0),
    MapObject("local_13", 23.89, 120.52, 8.0),
    MapObject("local_14", 24.44, 120.65, 8.0),
    MapObject("local_15", 24.62, 120.76, 8.0),
    MapObject("local_16", 24.83, 120.92, 8.0),
    MapObject("extra_0", 23.28, 120.31, 7.0),
    MapObject("extra_1", 23.55, 120.43, 7.0),
    MapObject("extra_2", 23.31, 120.33, 7.0),
    MapObject("extra_3", 24.70, 120.88, 7.0),
    MapObject("extra_4", 24.34, 120.60, 7.0),
    MapObject("extra_5", 23.57, 120.30, 7.0),
    MapObject("extra_6", 22.40, 120.58, 7.0),
)

/** Every id that has a painted icon, for loading them once per map. */
internal val MAP_OBJECT_IDS: List<String> by lazy {
    (com.starmist.core.route.TaiwanLoop.route.stops.mapNotNull { it.landmark?.id } +
        com.starmist.core.route.TaiwanLoop.route.pois.map { it.id } + LOCAL_OBJECTS.map { it.id }).distinct()
}
