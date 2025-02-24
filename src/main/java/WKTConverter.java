import com.esri.arcgisruntime.geometry.Polygon;


import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.OperatorExportToJson;
import com.esri.core.geometry.OperatorImportFromJson;
import com.esri.core.geometry.OperatorExportToWkt;
import com.esri.core.geometry.ogc.OGCGeometry;

public class WKTConverter {

    public static String convertToWKT(Polygon arcgisPolygon) {
        // Ensure the polygon is not null
        if (arcgisPolygon == null) {
            throw new IllegalArgumentException("Polygon cannot be null");
        }

        // Convert ArcGIS Runtime Polygon to JSON string
        String jsonString = arcgisPolygon.toJson();

        // Parse the JSON string to Esri Geometry API Geometry
        com.esri.core.geometry.Geometry esriGeometry = OperatorImportFromJson.local()
                .execute(Geometry.Type.Polygon, jsonString).getGeometry();

        // Create an OGCGeometry from the Esri Geometry
        OGCGeometry ogcGeometry = OGCGeometry.createFromEsriGeometry(esriGeometry, null);

        // Convert OGCGeometry to WKT
        return ogcGeometry.asText();
    }

        public static com.esri.arcgisruntime.geometry.Geometry fromWKT(String wkt, SpatialReference spatialReference) {
            // Parse the WKT string using the Esri Geometry API
            OGCGeometry ogcGeometry = OGCGeometry.fromText(wkt);

            // Convert the parsed geometry to JSON
            String json = OperatorExportToJson.local().execute(com.esri.core.geometry.SpatialReference.create(4326), ogcGeometry.getEsriGeometry());

            // Create and return the ArcGIS Runtime Geometry from JSON
            return com.esri.arcgisruntime.geometry.Geometry.fromJson(json, spatialReference);
        }


}
