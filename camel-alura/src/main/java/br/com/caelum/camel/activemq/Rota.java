package br.com.caelum.camel.activemq;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpMethods;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;

public class Rota {

	public static void main(String[] args) throws Exception {
		
		
		CamelContext context = new DefaultCamelContext();
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("admin","admin","tcp://0.0.0.0:61616");
        context.addComponent("activeMQ",JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

		
		//context.addComponent("activeMQ", ActiveMQComponent.activeMQComponent(connectionFactory));
		//context.addComponent("activeMQ", ActiveMQComponent.activeMQComponent("tpc://localhost:61616"));
	
		context.addRoutes(new RouteBuilder() {
			
			@Override
			public void configure() throws Exception {
				
				errorHandler(deadLetterChannel("activeMQ:queue:pedidos.DLQ")
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
				
				
					from("activeMQ:queue:pedidos")
					.log("${file:name}")
					.routeId("rota-pedidos")
					.delay(1000)
					.to("validator:pedido.xsd")
					.log("chegamos aqui")
					.multicast()
						.to("direct:soap")
							.log("Chamando soap com ${body}")
						.to("direct:http");
					
					
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
						.setHeader(Exchange.HTTP_METHOD, HttpMethods.GET)
						.setHeader(Exchange.HTTP_QUERY, 
							simple("ebookId=${property.ebookId}&pedidoId=${property.pedidoId}&clienteId=${property.email}"))
						.to("http4://localhost:8181/webservices/ebook/item");
					 
					 
					 
						from("direct:soap")
						.routeId("rota-soap")
							.to("xslt:pedido-para-soap.xslt")
							.log("${body}")
							.setHeader(Exchange.CONTENT_TYPE, constant("text/xml"))
						.to("http4://localhost:8181/webservices/financeiro");
			}
		});
		
		
		context.start();
		Thread.sleep(20000);
		context.stop();
		
	}
}
