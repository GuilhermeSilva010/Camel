package br.com.caelum.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;

public class RotasDesafio {
	//Pegando Informações em um endpoint e transformando em um arquivo xml, determinando um periodo de tempo para ping no endpoint
	//Podemos perder informações no InputStream caso o método convertBodyto não for utilizado
	
	
	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {

				from("timer://negociações?fixedRate=true&delay=1s&period=360s")
					.to("http4://argentumws-spring.herokuapp.com/negociacoes")
						.convertBodyTo(String.class)
						.log("${body}")
						.setHeader(Exchange.FILE_NAME, constant("negociacoes.xml"))
				.to("file:saida2");
			}
		});

		context.start();
		Thread.sleep(2000);
		context.stop();
		
	}

}
