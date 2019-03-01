import org.springframework.cloud.contract.spec.Contract
import org.springframework.http.MediaType

Contract.make {
    description("should return all the reservations :: ")
    request {
        description("Get all reservations Request::")
        url("/reservations")
        method(GET())
    }
    response {
        description("Get all reservations response ::")
        status(200)
        headers {
            contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        }
        body("""[{"reservationName":"A", "id":"1"}, { "reservationName" :"B", "id" : "2"}] """)
    }

}