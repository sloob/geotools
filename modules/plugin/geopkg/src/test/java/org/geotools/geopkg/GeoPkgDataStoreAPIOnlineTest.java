/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2010, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.geopkg;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.geotools.geometry.jts.LiteCoordinateSequenceFactory;
import org.geotools.jdbc.JDBCDataStoreAPIOnlineTest;
import org.geotools.jdbc.JDBCDataStoreAPITestSetup;
import org.geotools.jdbc.JDBCDataStoreOnlineTest;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

public class GeoPkgDataStoreAPIOnlineTest extends JDBCDataStoreAPIOnlineTest {

    public GeoPkgDataStoreAPIOnlineTest() {
        this.forceLongitudeFirst = true;
    }

    @Override
    protected JDBCDataStoreAPITestSetup createTestSetup() {
        return new GeoPkgDataStoreAPITestSetup(true);
    }

    @Override
    public void testGetFeaturesWriterAdd() throws IOException {
        // cannot work because the resultset metadata is not available on an empty resultset
        // see: https://www.sqlite.org/datatype3.html
        // and: https://github.com/xerial/sqlite-jdbc/issues/112
    }

    @Override
    public void testTransactionIsolation() throws Exception {
        // sqlite does not allow two transactions from two different connections writing 
        // at the same time
    }
    
    public void testDistanceSimplification() throws Exception {
        SimpleFeatureSource fs = dataStore.getFeatureSource(tname("road"));
        assertTrue(fs.getSupportedHints().contains(Hints.GEOMETRY_DISTANCE));

        FilterFactory factory = dataStore.getFilterFactory();
        Query q = new Query(tname("road"), factory.id(Collections.singleton(factory.featureId("road.0"))));
        Hints hints = new Hints(Hints.GEOMETRY_DISTANCE, new Double(10));
        q.setHints(hints);

        try(SimpleFeatureIterator it = fs.getFeatures(q).features()) {
            it.hasNext();

            // original feature:
            //      0 | LINESTRING( 1 1, 2 2, 4 2, 5 1 );srid=4326 | "r1"
            // but geometry has been simplified to its bbox (which is ok, no need to respect connectivity,
            // same logic as in ShapefileDataStore
            SimpleFeature f = it.next();
            LineString ls = (LineString) f.getDefaultGeometry();
            assertEquals(2, ls.getNumPoints());
            assertEquals(new Coordinate(1, 1), ls.getStartPoint().getCoordinate());
            assertEquals(new Coordinate(5, 2), ls.getEndPoint().getCoordinate());
        }

    }

}
