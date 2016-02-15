

public class RandomVariables {
	
	// 
		// This file contains programs for generating random numbers with
		// uniform distributions and exponential distributions.
		//
		
		double Seed = 1111.0;

		/*******************************************/
		/* returns a uniform (0,1) random variable */
		/*******************************************/
		double uniform_rv()           
		{
		    double constant_K = 16807.0;
		    double constant_M = 2.147483647e9;
		    double rv;

		    
		    Seed = (constant_K * Seed) % constant_M;
		    rv = (double)Seed / constant_M;
		    return(rv);
		}

		/*******************************/
		/* given arrival rate lambda   */
		/* returns an exponential r.v. */ 
		/*******************************/
		double exponential_rv(double lambda)
		{
		    double exp;
		    exp = (double)( ((-1) / lambda) * Math.log(uniform_rv()) );
		    return(exp);
		}

}
