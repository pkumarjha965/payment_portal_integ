package org.medix.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.medix.model.Product;

import java.util.List;
import java.util.Map;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static final List<Product> CATALOG = List.of(
            new Product(
                    "prod_1",
                    "AeroSound Pro Wireless ANC Headphones",
                    "Audio",
                    "Hybrid active noise cancelling with 40-hour battery life and spatial audio support.",
                    12999L, // $129.99
                    4.8,
                    342,
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&q=80",
                    true,
                    "Best Seller"
            ),
            new Product(
                    "prod_2",
                    "PulseFit Ultra Smartwatch",
                    "Wearables",
                    "AMOLED always-on display, ECG, blood oxygen monitor, and 7-day battery life.",
                    19999L, // $199.99
                    4.7,
                    215,
                    "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&q=80",
                    true,
                    "New"
            ),
            new Product(
                    "prod_3",
                    "Apex Mechanical RGB Gaming Keyboard",
                    "Gaming",
                    "Hot-swappable tactile switches, per-key RGB backlighting, and aluminum frame.",
                    8999L, // $89.99
                    4.9,
                    512,
                    "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&q=80",
                    true,
                    "Top Rated"
            ),
            new Product(
                    "prod_4",
                    "VividSound 360 Waterproof Bluetooth Speaker",
                    "Audio",
                    "IPX7 waterproof rating, 24-hour playtime, punchy bass, and stereo pairing.",
                    4999L, // $49.99
                    4.6,
                    188,
                    "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&q=80",
                    true,
                    "Popular"
            ),
            new Product(
                    "prod_5",
                    "Nexus 8-in-1 USB-C Thunderbolt Dock",
                    "Accessories",
                    "Dual 4K@60Hz HDMI, 100W Power Delivery pass-through, Gigabit Ethernet, and SD slots.",
                    6499L, // $64.99
                    4.8,
                    94,
                    "https://images.unsplash.com/photo-1622434641406-a158123450f9?w=600&q=80",
                    true,
                    "Essential"
            ),
            new Product(
                    "prod_6",
                    "CyberGrip Wireless Precision Ergonomic Mouse",
                    "Accessories",
                    "4000 DPI high-precision sensor, multi-device Bluetooth switching, quiet clicks.",
                    3999L, // $39.99
                    4.5,
                    140,
                    "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&q=80",
                    true,
                    "Sale"
            )
    );

    @GET
    public Response getAllProducts() {
        return Response.ok(CATALOG).build();
    }

    @GET
    @Path("/{id}")
    public Response getProductById(@PathParam("id") String id) {
        return CATALOG.stream()
                .filter(p -> p.id().equalsIgnoreCase(id))
                .findFirst()
                .map(p -> Response.ok(p).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Product not found: " + id))
                        .build());
    }
}
