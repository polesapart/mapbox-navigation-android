package com.mapbox.navigation.ui.internal.building;

import android.graphics.Color;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.sources.VectorSource;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.distance;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;


/**
 * This layer handles the creation and customization of a {@link FillExtrusionLayer}
 * to show 3D buildings. For now, this layer is only compatible with the Mapbox
 * Streets v8 vector tile source. See [https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/]
 * (https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/) for more information
 * about the Mapbox Streets v8 vector tile source.
 */
abstract class BuildingLayer {

  public static final String BUILDING_VECTOR_SOURCE_ID = "building-vector-source-id";
  public static final String BUILDING_LAYER_ID = "building";
  public static final Integer DEFAULT_COLOR = Color.RED;
  public static final Float DEFAULT_OPACITY = 1f;
  public static final Float QUERY_DISTANCE_MAX_METERS = 1f;
  private final MapboxMap mapboxMap;

  public BuildingLayer(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        VectorSource buildingVectorSource = style.getSourceAs(BUILDING_VECTOR_SOURCE_ID);
        if (buildingVectorSource == null) {
          addVectorSourceToStyle();
        }
      }
    });
  }

  /**
   * Adds the Mapbox Streets {@link VectorSource} to the {@link MapboxMap}'s {@link Style} object.
   */
  private void addVectorSourceToStyle() {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        VectorSource buildingFootprintVectorSource = new VectorSource(
            BUILDING_VECTOR_SOURCE_ID, "mapbox://mapbox.mapbox-streets-v8");
        style.addSource(buildingFootprintVectorSource);
      }
    });
  }

  /**
   * Gets the correct {@link Expression#all(Expression...)} expression to show the building
   * extrusion associated with the query {@link LatLng}.
   *
   * @param queryLatLng the {@link LatLng} to use in determining which building is closest to the coordinate.
   * @return an {@link Expression#all(Expression...)} expression
   */
  protected Expression getFilterExpression(LatLng queryLatLng) {
    return all(
        eq(get("extrude"), "true"),
        eq(get("type"), "building"),
        eq(get("underground"), "false"),
        lt(distance(Point.fromLngLat(queryLatLng.getLongitude(), queryLatLng.getLatitude())),
            literal(QUERY_DISTANCE_MAX_METERS)));
  }
}
