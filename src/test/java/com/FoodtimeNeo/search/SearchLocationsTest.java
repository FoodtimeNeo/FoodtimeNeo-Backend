package com.FoodtimeNeo.search;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class SearchLocationsTest {
    @Test void validatesPointsAndComputesWgs84DistancesWithoutPersistingDeviceLocation() {
        UUID id=UUID.randomUUID();var locations=new SearchLocations(Map.of(id,new SearchLocations.Point(39.95,116.34,"test")));
        assertThat(locations.distances(39.95,116.34).get(id)).isZero();
        assertThat(SearchLocations.meters(0,0,1,0)).isBetween(111000d,112000d);
        assertThat(locations.distances(null,null)).isEmpty();
        assertThatThrownBy(()->locations.distances(Double.NaN,0d)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->new SearchLocations(Map.of(id,new SearchLocations.Point(999d,0d,"test")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new SearchLocations(Map.of(id,new SearchLocations.Point(0d,0d,"")))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new SearchLocations(Map.of(id,new SearchLocations.Point(null,0d,"test")))).isInstanceOf(IllegalArgumentException.class);
    }
}
