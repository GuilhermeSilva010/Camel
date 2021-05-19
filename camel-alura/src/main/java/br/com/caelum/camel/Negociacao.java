package br.com.caelum.camel;

import java.util.Calendar;

public class Negociacao {

	private int quantidade;
	private Double preco;
	private Calendar data;
	
	public int getQuantidade() {
		return quantidade;
	}
	public void setQuantidade(int quantidade) {
		this.quantidade = quantidade;
	}
	public Double getPreco() {
		return preco;
	}
	public void setPreco(Double preco) {
		this.preco = preco;
	}
	public Calendar getData() {
		return data;
	}
	public void setData(Calendar data) {
		this.data = data;
	}
	
	
	@Override
	public String toString() {
		return "Negociacao [preco=" + preco + ", quantidade=" + quantidade + "]";
	}
	
	
}
