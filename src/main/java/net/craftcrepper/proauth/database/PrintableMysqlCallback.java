package net.craftcrepper.proauth.database;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class PrintableMysqlCallback<T> implements MysqlCallback<T>{

	private String toPrint;
	
	@Override
	public void onResult(T result, Throwable error) {
		if (error != null){
			if (toPrint != null)
				System.out.println(toPrint);
			error.printStackTrace();
		}
	}
	
}
