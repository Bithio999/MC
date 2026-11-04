package dev.heavenlyrestriction.cartography;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class MapViewTest {
  @Test void cursorStaysOnTheSameBlockDuringZoom(){var v=new MapView();v.center(-231.25,812.5);double x=v.worldX(740,200,800),z=v.worldZ(173,40,500);v.zoom(3,740,173,200,40,800,500);assertEquals(x,v.worldX(740,200,800),1e-9);assertEquals(z,v.worldZ(173,40,500),1e-9);}
  @Test void dragFollowsCursorAtEveryScale(){for(double scale:new double[]{.125,.5,1,4,8}){var v=new MapView();v.scale=scale;v.center(193,-300);double before=v.screenX(234,100,700);v.drag(43,-18);assertEquals(before+43,v.screenX(234,100,700),1e-9);assertEquals(-300+18/scale,v.z,1e-9);}}
  @Test void negativeWorldCoordinatesRoundTrip(){var v=new MapView();v.scale=.375;v.center(-20000000,-103.5);assertEquals(-20000314.25,v.worldX(v.screenX(-20000314.25,183,523),183,523),1e-8);assertEquals(-152,v.worldZ(v.screenZ(-152,44,319),44,319),1e-9);}
  @Test void zoomLimitsCannotOverflow(){var v=new MapView();for(int n=0;n<100;n++)v.zoom(99,10,10,0,0,500,300);assertEquals(MapView.MAX_SCALE,v.scale);for(int n=0;n<100;n++)v.zoom(-99,10,10,0,0,500,300);assertEquals(MapView.MIN_SCALE,v.scale);assertTrue(Double.isFinite(v.x));}
  @Test void invalidCoordinatesCannotPoisonTheView(){var v=new MapView();v.center(50,20);v.center(Double.NaN,40);assertEquals(50,v.x);assertEquals(20,v.z);v.center(Double.MAX_VALUE,-Double.MAX_VALUE);assertEquals(MapView.LIMIT,v.x);assertEquals(-MapView.LIMIT,v.z);v.zoom(Double.POSITIVE_INFINITY,5,5,0,0,100,100);assertEquals(1,v.scale);}
}
