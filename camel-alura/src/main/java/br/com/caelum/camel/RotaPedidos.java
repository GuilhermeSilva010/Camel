package br.com.caelum.camel;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.impl.DefaultCamelContext;
import org.xml.sax.SAXParseException;

public class RotaPedidos {

	public static void main(String[] args) throws Exception {

		CamelContext context = new DefaultCamelContext();
		context.addComponent("activemq", ActiveMQComponent.activeMQComponent("tpc://localhost:61616"));
		
		
		context.addRoutes(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {

//				TRATAMENTO DE ERRO E FAZENDO REDELIVERY, CRIANDO PASTA PARA OS ARQUIVOS DE ERROS
//				
//				onException(SAXParseException.class)
//					.handled(true)
				
					errorHandler(deadLetterChannel("activemq:queue:pedidos.DLQ")
					.logExhaustedMessageHistory(true)
					.maximumRedeliveries(3)
					.redeliveryDelay(2000)
					.onRedelivery(new Processor() {
						
						@Override
						public void process(Exchange exchange) throws Exception {
							int counter =(int)exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER);
							int max =(int)exchange.getIn().getHeader(Exchange.REDELIVERY_MAX_COUNTER);
							
							System.out.println("REDELIVERY " + counter + "/" + max);
						}
					}));
					
				//Entrada padrao de arquivos
				 //from("file:pedidos?delay=5s&noop=true")
				
				//Entrada do ACTIVEMQ
					
				 from("activemq:queue:pedidos")
				 
				 
				 .routeId("rota-pedidos")
				 .to("validator:pedido.xsd")
				 
				 
				 //Criando subrotase com MultiCast dando id para as rotas para poder identificar se houver erro
				 //E criando rotas com arquitetura SEDA, quando usar SEDA não precisa de multicast, que cria filas para cada rota e subrota
				 
				 .multicast()
				 	.to("direct:soap")
					.to("direct:http");
				
				//Rota http declarando variaveis com property, separando o conteudo com split
				//E filtrando o conteudo com filter. Usando marshal para transformar o arquivo xml em json
				//Mudando o Header e dizendo o metodo http a ser usado, e fazendo uma query com a variaveis declaradas para pegar o conteudo do arquivo 
				 
				 from("direct:http")
				 .routeId("rota-http")
					.setProperty("pedidoId", xpath("/pedido/id/text()"))
					.setProperty("email", xpath("/pedido/pagamento/email-titular/text()"))
					.split()
						.xpath("/pedido/itens/item")
					.filter()
						.xpath("/item/formato[text()='EBOOK']")
					.setProperty("ebookId", xpath("/item/livro/codigo/text()"))	
					.marshal().xmljson()
					.log("${id} - ${body}")
					//.setHeader("CamelFileName", simple("${file:name.noext}.json"))
					//.setHeader(Exchange.FILE_NAME, simple("${file:name.noext} - ${header.CamelSplitIndex}.json"))
					.setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
					.setHeader(Exchange.HTTP_QUERY, 
						simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.email}"))
					.to("http4://localhost:8080/webservices/ebook/item");
				
					from("direct:soap")
					.routeId("rota-soap")
						.to("xslt:pedido-para-soap.xslt")
						.log("${body}")
						.setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
					.to("http4://localhost:8080/webservices/financeiro");
			}
		});

		context.start();
		Thread.sleep(2000);
		context.stop();
		
	}	
}
