package com.mapbox.navigation.ui.internal.building;

import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.navigation.utils.NavigationException;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * This layer handles the creation and customization of a {@link FillLayer}
 * to highlight the footprint of an individual building. For now, this layer is only
 * compatible with the Mapbox Streets v8 vector tile source. See
 * [https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/]
 * (https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/) for more information
 * about the Mapbox Streets v8 vector tile source.
 */
public class BuildingFootprintHighlightLayer extends BuildingLayer {

  public static final String BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID = "building-highlighted-footprint-layer-id";
  private LatLng queryLatLng;
  private Integer color;
  private Float opacity;
  private MapboxMap mapboxMap;

  public BuildingFootprintHighlightLayer(MapboxMap mapboxMap) {
    super(mapboxMap);
    this.mapboxMap = mapboxMap;
  }

  /**
   * Toggles the visibility of the building footprint highlight layer.
   *
   * @param visible true if the layer should be displayed. False if it should be hidden.
   */
  public void updateVisibility(final boolean visible) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintLayer == null && visible) {
          if (queryLatLng == null) {
            throw new NavigationException("BuildingHighlightLayer's queryLatLng is null. Set"
                + " the query LatLng before you set the footprint's visibility to true");
          } else {
            addFootprintHighlightFillLayerToMap(queryLatLng);
          }
        } else if (buildingFootprintLayer != null) {
          buildingFootprintLayer.setProperties(visibility(visible ? VISIBLE : NONE));
        }
      }
    });
  }

  /**
   * Set the {@link LatLng} location of the building highlight layer. The {@link LatLng} passed
   * through this method is used to see whether its within the footprint of a specific
   * building. If so, that building's footprint is used for a 2D highlighted footprint.
   *
   * @param queryLatLng the new coordinates to use in querying the building layer
   *                    to get the associated {@link Polygon} to eventually highlight.
   */
  public void setQueryLatLng(final LatLng queryLatLng) {
    this.queryLatLng = queryLatLng;
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintLayer != null) {
          buildingFootprintLayer.setFilter(getFilterExpression(queryLatLng));
        }
      }
    });
  }

  /**
   * Set the color of the building highlight layer.
   *
   * @param newColor the new color value
   */
  public void setColor(final int newColor) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.withProperties(fillColor(newColor));
        }
        color = newColor;
      }
    });
  }

  /**
   * Set the opacity of the building highlight layer.
   *
   * @param newOpacity the new opacity value
   */
  public void setOpacity(final Float newOpacity) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.withProperties(fillOpacity(newOpacity));
        }
        opacity = newOpacity;
      }
    });
  }

  /**
   * Retrieve the latest set color of the building highlight layer.
   *
   * @return the color Integer
   */
  public Integer getColor() {
    return color;
  }

  /**
   * Retrieve the latest set opacity of the building highlight layer.
   *
   * @return the opacity Float
   */
  public Float getOpacity() {
    return opacity;
  }

  /**
   * Retrieve the latest set opacity of the building highlight layer.
   *
   * @return the opacity Float
   */
  public LatLng getQueryLatLng() {
    return queryLatLng;
  }

  /**
   * Customize and add a {@link FillLayer} to the map to show a highlighted
   * building footprint.
   */
  private void addFootprintHighlightFillLayerToMap(LatLng queryLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = new FillLayer(
            BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID, BUILDING_VECTOR_SOURCE_ID);
        buildingFootprintFillLayer.setSourceLayer(BUILDING_LAYER_ID);
        buildingFootprintFillLayer.setFilter(getFilterExpression(queryLatLng));
        buildingFootprintFillLayer.withProperties(
            fillColor(color == null ? DEFAULT_COLOR : color),
            fillOpacity(opacity == null ? DEFAULT_OPACITY : opacity)
        );
        if (style.getLayerAs(BUILDING_LAYER_ID) != null) {
          style.addLayerAbove(buildingFootprintFillLayer, BUILDING_LAYER_ID);
        } else {
          style.addLayer(buildingFootprintFillLayer);
        }
      }
    });
  }
}
