package UnitTesting;

import Model.Exceptions.RoutingException;
import NetworkLayer.NetworkLayer;
import NetworkLayer.NetworkingDAO;
import NetworkLayer.NetworkLayer.RouteEntry;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkLayerTest {

    NetworkLayer networkLayer;

    @BeforeEach
    void setUp() {
        networkLayer = NetworkLayer.getSingleton();
        networkLayer.getAllRoutes().clear(); // Clear routing table before each test
        NetworkingDAO.getInstance().updateNode(1); // or 2, depending on neighbor used in test
    }

    @AfterEach
    void tearDown() {
        networkLayer.getAllRoutes().clear(); // Clean up after each test
    }

    @Test
    void getSingleton_shouldReturnSameInstance() {
        NetworkLayer instance1 = NetworkLayer.getSingleton();
        NetworkLayer instance2 = NetworkLayer.getSingleton();
        assertSame(instance1, instance2);
    }

    @Test
    void updateRoute_shouldAddNewRoute() throws RoutingException {
        networkLayer.updateRoute(2, 1, 3);
        RouteEntry route = networkLayer.getRoute(2);
        assertNotNull(route);
        assertEquals(1, route.nextHop);
        assertEquals(3, route.cost);
    }

    @Test
    void updateRoute_shouldOverwriteRouteIfExists() throws RoutingException {
        networkLayer.updateRoute(2, 1, 3);
        networkLayer.updateRoute(2, 4, 1);
        RouteEntry route = networkLayer.getRoute(2);
        assertEquals(4, route.nextHop);
        assertEquals(1, route.cost);
    }

    @Test
    void getRoute_shouldReturnNullIfNotPresent() throws RoutingException {
        assertNull(networkLayer.getRoute(5));
    }

    @Test
    void removeRoute_shouldDeleteExistingRoute() throws RoutingException {
        networkLayer.updateRoute(3, 1, 2);
        assertNotNull(networkLayer.getRoute(3));
        networkLayer.removeRoute(3);
        assertNull(networkLayer.getRoute(3));
    }

    @Test
    void getAllRoutes_shouldReturnAllRoutes() {
        networkLayer.updateRoute(1, 2, 1);
        networkLayer.updateRoute(3, 2, 2);

        Map<Integer, RouteEntry> routes = networkLayer.getAllRoutes();
        assertEquals(2, routes.size());
        assertTrue(routes.containsKey(1));
        assertTrue(routes.containsKey(3));
    }

    @Test
    void handleDVUpdate_shouldAddNewRoute() throws RoutingException {
        // Simulate a DV update from neighbor 1
        networkLayer.handleDVUpdate(1, Map.of(4, 2)); // cost to 4 from neighbor = 2
        RouteEntry route = networkLayer.getRoute(4);

        assertNotNull(route);
        assertEquals(1, route.nextHop);
        assertEquals(3, route.cost); // 1 (hop to neighbor) + 2
    }

    @Test
    void handleDVUpdate_shouldUpdateExistingRouteIfCheaper() throws RoutingException {
        // Set up an initial worse route
        networkLayer.updateRoute(4, 2, 3);
        networkLayer.handleDVUpdate(1, Map.of(4, 1)); // new cheaper path: 1 + 1 = 3

        RouteEntry route = networkLayer.getRoute(4);
        assertEquals(1, route.nextHop);
        assertEquals(2, route.cost);
    }

    @Test
    void handleDVUpdate_shouldRemoveRouteIfUnreachable() throws RoutingException {
        // Initially reachable through neighbor 1
        networkLayer.updateRoute(4, 1, 3);
        networkLayer.handleDVUpdate(1, Map.of(4, 10)); // 10 is >= INFINITY (4), remove route

        assertNull(networkLayer.getRoute(4));
    }

    @Test
    void handleDVUpdate_shouldNotUpdateToWorseRoute() throws RoutingException {
        // Existing better route
        networkLayer.updateRoute(4, 1, 2);
        networkLayer.handleDVUpdate(2, Map.of(4, 3)); // 1 + 3 = 4, not better

        RouteEntry route = networkLayer.getRoute(4);
        assertEquals(1, route.nextHop); // should remain same
        assertEquals(2, route.cost);
    }
}
