package com.lch;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lch.workflow.BranchComponent;
import com.lch.workflow.Computer;
import com.lch.workflow.ComputerComponent;
import com.lch.workflow.EndComponent;
import com.lch.workflow.Filter;
import com.lch.workflow.StartComponent;
import com.lch.workflow.WorkFlow;
import com.lch.workflow.impl.DefaultComputerComponent;
import com.lch.workflow.impl.DefaultEndComponent;
import com.lch.workflow.impl.DefaultFilterComponent;
import com.lch.workflow.impl.DefaultStartComponent;
import com.lch.workflow.impl.DefaultWorkFlow;

@RestController
public class FilterFlowController {

	private final static Logger LOGGER = LoggerFactory.getLogger(FilterFlowController.class);


	@RequestMapping(value = "/test/{count}")
	public Float counttest(@PathVariable(value = "count") String count) {
		LOGGER.info("/test/" + count);
		final int size = Integer.valueOf(count);
		
		final WorkFlow workflow = new DefaultWorkFlow("TEST-WORKFLOW");
		LOGGER.info("first start component");
		StartComponent<Float> startComponent = new DefaultStartComponent<Float>(workflow, "START COMPONENT") {

			@Override
			public void process() {
				Random rand = new Random();
				for(int i = 0; i < size; i++){
					Float f = rand.nextFloat();
					this.write(f);
				}
			}
		};
		workflow.registerStart(startComponent);
		LOGGER.info("send component");
		BranchComponent<Float> secondComponent_1 = new DefaultFilterComponent<Float>(workflow, "SECOND_1");
		secondComponent_1.setFilter(new Filter<Float>() {

			@Override
			public boolean filte(Float t) {
				return t < 0.5;
			}
		});
		
		BranchComponent<Float> secondComponent_2 = new DefaultFilterComponent<Float>(workflow, "SECOND_2");
		secondComponent_2.setFilter(new Filter<Float>() {

			@Override
			public boolean filte(Float t) {
				return t >= 0.5;
			}
		});		
		
		LOGGER.info("register");
		startComponent.registe(secondComponent_1);
		startComponent.registe(secondComponent_2);
		
		ComputerComponent<Float, Float> computer_1 = new DefaultComputerComponent<Float, Float>(workflow, "COMPUTER_1");
		ComputerComponent<Float, Float> computer_2 = new DefaultComputerComponent<Float, Float>(workflow, "COMPUTER_2");
		
		Filter<Float> alwaystrue = new Filter<Float>() {

			@Override
			public boolean filte(Float t) {
				return true;
			}
		};
		Computer<Float, Float> computer = new Computer<Float, Float>() {

			@Override
			public Float compute(Float t) {
				return t * 0.003F;
			}
		};
		computer_1.setFilter(alwaystrue);
		computer_2.setFilter(alwaystrue);
		computer_1.setComputer(computer);
		computer_2.setComputer(computer);
		
		secondComponent_1.registe(computer_1);
		secondComponent_2.registe(computer_2);
		
				
		LOGGER.info("end component");
		EndComponent<Float> endComponent = new DefaultEndComponent<Float>(workflow, "END COMPONENT");
		
		computer_1.registe(endComponent);
		computer_2.registe(endComponent);
		
		BlockingQueue<Float> queue = workflow.registerEnd(endComponent);
		
		LOGGER.info("workflow starting...");
		new Thread(workflow).start();
		
		float result = 0.0F;
		
		LOGGER.info("get element form " + queue);
		
		while(!workflow.isDone()){
			try {
				Float f = queue.poll(1, TimeUnit.SECONDS);
				if(f != null){
					result += f;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		LOGGER.info("controller return");
		return result;
	}

}
