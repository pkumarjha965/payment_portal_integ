package org.medix.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.medix.dto.shipping.CreateShipmentRequest;
import org.medix.dto.shipping.ShippingRateRequest;
import org.medix.dto.shipping.ShippingRateResponse;
import org.medix.model.ShipmentRecord;
import org.medix.service.ShippingService;

import java.util.Map;

@Path("/api/shipping")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShippingResource {

    @Inject
    ShippingService shippingService;

    @POST
    @Path("/rates")
    public Response getRates(ShippingRateRequest request) {
        ShippingRateResponse response = shippingService.calculateRates(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/shipments")
    public Response createShipment(CreateShipmentRequest request) {
        ShipmentRecord record = shippingService.createShipment(request);
        return Response.status(Response.Status.CREATED).entity(record).build();
    }

    @GET
    @Path("/track/{trackingNumber}")
    public Response getTracking(@PathParam("trackingNumber") String trackingNumber) {
        return shippingService.getTracking(trackingNumber)
                .map(res -> Response.ok(res).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Tracking number not found: " + trackingNumber))
                        .build());
    }

    @GET
    @Path("/order/{orderId}")
    public Response getShipmentByOrderId(@PathParam("orderId") String orderId) {
        return shippingService.getTrackingByOrderId(orderId)
                .map(res -> Response.ok(res).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No shipment found for order: " + orderId))
                        .build());
    }

    @GET
    @Path("/payment/{paymentId}")
    public Response getShipmentByPaymentId(@PathParam("paymentId") String paymentId) {
        return shippingService.getTrackingByPaymentId(paymentId)
                .map(res -> Response.ok(res).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "No shipment found for payment ID: " + paymentId))
                        .build());
    }

    @POST
    @Path("/track/{trackingNumber}/advance")
    public Response advanceTracking(@PathParam("trackingNumber") String trackingNumber) {
        return shippingService.advanceTrackingStatus(trackingNumber)
                .map(res -> Response.ok(res).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Shipment not found for tracking: " + trackingNumber))
                        .build());
    }

    @GET
    @Path("/label/{trackingNumber}")
    @Produces("image/svg+xml")
    public Response getLabelSvg(@PathParam("trackingNumber") String trackingNumber) {
        var opt = shippingService.getTracking(trackingNumber);
        String carrier = opt.map(t -> t.carrier()).orElse("STANDARD CARRIER");
        String recipient = opt.map(t -> t.recipientName()).orElse("Recipient");
        String destination = opt.map(t -> t.destination()).orElse("123 Tech Blvd, San Jose, CA");
        String status = opt.map(t -> t.status()).orElse("ACTIVE");

        String svg = String.format("""
            <svg xmlns="http://www.w3.org/2000/svg" width="450" height="300" viewBox="0 0 450 300" style="background:#fff; font-family:sans-serif; border: 2px solid #000;">
              <!-- Header -->
              <rect x="0" y="0" width="450" height="50" fill="#111827"/>
              <text x="20" y="32" fill="#fff" font-size="20" font-weight="bold">PRIORITY PARCEL - %s</text>
              <text x="350" y="32" fill="#10B981" font-size="14" font-weight="bold">%s</text>

              <!-- Sender & Receiver -->
              <text x="20" y="75" font-size="10" fill="#6B7280">SHIP FROM:</text>
              <text x="20" y="90" font-size="12" font-weight="bold">MEDIX E-COMMERCE LOGISTICS</text>
              <text x="20" y="105" font-size="11">500 Howard St, Suite 300, San Francisco, CA 94105</text>

              <line x1="20" y1="115" x2="430" y2="115" stroke="#E5E7EB" stroke-width="2"/>

              <text x="20" y="135" font-size="10" fill="#6B7280">DELIVER TO:</text>
              <text x="20" y="155" font-size="16" font-weight="bold">%s</text>
              <text x="20" y="175" font-size="13">%s</text>

              <!-- Barcode Simulation -->
              <line x1="20" y1="195" x2="430" y2="195" stroke="#000" stroke-width="1"/>
              <g transform="translate(40, 205)">
                <!-- Barcode bars -->
                <rect x="0" y="0" width="4" height="50" fill="#000"/>
                <rect x="8" y="0" width="2" height="50" fill="#000"/>
                <rect x="14" y="0" width="6" height="50" fill="#000"/>
                <rect x="24" y="0" width="3" height="50" fill="#000"/>
                <rect x="30" y="0" width="5" height="50" fill="#000"/>
                <rect x="40" y="0" width="2" height="50" fill="#000"/>
                <rect x="46" y="0" width="7" height="50" fill="#000"/>
                <rect x="58" y="0" width="3" height="50" fill="#000"/>
                <rect x="65" y="0" width="4" height="50" fill="#000"/>
                <rect x="73" y="0" width="2" height="50" fill="#000"/>
                <rect x="80" y="0" width="6" height="50" fill="#000"/>
                <rect x="92" y="0" width="3" height="50" fill="#000"/>
                <rect x="100" y="0" width="5" height="50" fill="#000"/>
                <rect x="110" y="0" width="2" height="50" fill="#000"/>
                <rect x="116" y="0" width="6" height="50" fill="#000"/>
                <rect x="126" y="0" width="4" height="50" fill="#000"/>
                <rect x="135" y="0" width="5" height="50" fill="#000"/>
                <rect x="145" y="0" width="2" height="50" fill="#000"/>
                <rect x="152" y="0" width="7" height="50" fill="#000"/>
                <rect x="165" y="0" width="3" height="50" fill="#000"/>
                <rect x="174" y="0" width="4" height="50" fill="#000"/>
                <rect x="182" y="0" width="2" height="50" fill="#000"/>
                <rect x="190" y="0" width="6" height="50" fill="#000"/>
                <rect x="202" y="0" width="3" height="50" fill="#000"/>
                <rect x="210" y="0" width="5" height="50" fill="#000"/>
                <rect x="220" y="0" width="2" height="50" fill="#000"/>
                <rect x="226" y="0" width="6" height="50" fill="#000"/>
                <rect x="236" y="0" width="4" height="50" fill="#000"/>
                <rect x="245" y="0" width="5" height="50" fill="#000"/>
                <rect x="255" y="0" width="3" height="50" fill="#000"/>
                <rect x="265" y="0" width="6" height="50" fill="#000"/>
                <rect x="277" y="0" width="2" height="50" fill="#000"/>
                <rect x="285" y="0" width="5" height="50" fill="#000"/>
                <rect x="295" y="0" width="4" height="50" fill="#000"/>
                <rect x="305" y="0" width="6" height="50" fill="#000"/>
                <rect x="317" y="0" width="3" height="50" fill="#000"/>
                <rect x="325" y="0" width="5" height="50" fill="#000"/>
                <rect x="335" y="0" width="4" height="50" fill="#000"/>
                <rect x="345" y="0" width="7" height="50" fill="#000"/>
                <rect x="358" y="0" width="2" height="50" fill="#000"/>
                <rect x="365" y="0" width="5" height="50" fill="#000"/>
              </g>
              <text x="225" y="275" font-size="14" font-weight="bold" text-anchor="middle" letter-spacing="3">%s</text>
            </svg>
            """, carrier, status, recipient, destination, trackingNumber);

        return Response.ok(svg).build();
    }
}
