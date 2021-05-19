package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class RotaSimples {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {

				from("file:pedidos?delay=5s&noop=true")
				.split()
					.xpath("/pedido/itens/item")
				.filter()
					.xpath("/item/formato[text()='EBOOK']")
				.marshal()
					.xmljson()
					.log("${id} - ${body}")
						// .setHeader("CamelFileName", simple("${file:name.noext}.json"))
				.setHeader(Exchange.FILE_NAME, simple("${file:name.noext} - ${header.CamelSplitIndex}.json"))
						// .setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
				.to("file:saida");

			}
		});

		context.start();
		Thread.sleep(2000);
		context.stop();
	}
}