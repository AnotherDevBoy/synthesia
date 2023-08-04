package io.synthesia;

import io.javalin
        .Javalin;
import io.synthesia.api.SignApi;
import io.synthesia.client.CryptoClient;
import io.synthesia.client.HttpCryptoClient;

import java.net.http.HttpClient;
import java.time.Duration;

public class App
{
    public static void main( String[] args )
    {
        var client = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        CryptoClient cryptoService = new HttpCryptoClient(client, Configuration.getSynthesiaApiKey());
        SignApi signApi = new SignApi(cryptoService);

        var app = Javalin.create()
                .post("/crypto/sign", signApi::sign)
                .start(7070);
    }
}
