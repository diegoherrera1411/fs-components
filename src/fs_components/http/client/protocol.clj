(ns fs-components.http.client.protocol)

(defprotocol HttpClient
  (conn-manager [this] "Get the connection manager for the current client")
  (http-client [this http-client-kw] "Get the http client for the client kw")
  (req! [http-client http-op-kw params]
    "Function that performs a http request according to the specification in the :http-client
    section in the config file, under the keyword http-op-kw

    A http-op specification includes:
    :base-url -> Url/kw for the host of the service to be required
    :path -> Path to the route to be requested (e. /service/:some-param/to/call)
    :method -> One of :get, :post, :put, :delete

    The params for the request include
    :body -> Body of the request (will be parsed to JSON)
    :query-params -> Query params of the request
    :path-params -> Path params of the request (e. :some-param 'my param value')
    :headers -> Additional headers to the request
    :timeout -> Timeout of the request
    :retries -> Number of retries of the request in case of failure

    Responds with a map that has
    :status -> Status of the request from server
    :body -> Payload of the response (parsed as edn by default)
    :headers -> headers of the response"))
