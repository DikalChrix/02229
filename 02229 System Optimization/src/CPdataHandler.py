from math import gcd
import pandas as pd
import numpy as np

def CPDataHandler(file):
    df = load_data(file)
    task_data = add_tuples(df)
    return task_data
    


def add_tuples(df):
    lcm = 1
    for i in df['period']:
        lcm = lcm*i//gcd(lcm, i)
    task_data = []
    for ind in df.index:
        task = []
        if df['type'][ind] != "ET":
            for i in range(int(lcm/df['period'][ind])):
                task.append((i*df['deadline'][ind],df['duration'][ind],(1+i)*df['deadline'][ind],df['priority'][ind]))
        else:
            #calculate MIT and add ET task
            for i in range(int(lcm/df['period'][ind])):
                if i*df['period'][ind]+df['deadline'][ind]<lcm:
                    task.append((i*df['period'][ind],df['duration'][ind],i*df['period'][ind]+df['deadline'][ind],df['priority'][ind]))
            #print(f"taskname {df['name'][ind]:<6} period/deadline: {df['period'][ind]} / {df['deadline'][ind]}")
        task_data.append(task)
    return task_data

def init_df(df):
    if 'duration' and 'period' and 'priority' and 'deadline' in df.columns:
        df['duration'] = df['duration'].apply(np.int32)
        df['period'] = df['period'].apply(np.int32)
        df['priority'] = df['priority'].apply(np.int32)
        df['deadline'] = df['deadline'].apply(np.int32)
    else:
        raise Exception("Dataframe missing fields")  
    return df
    
def load_data(file):
    df_comma =  pd.read_csv(file) #with seperation
    df_semi = pd.read_csv(file, sep=';') #without seperation
    df = pd.DataFrame()
    if df_comma.shape[1]>df_semi.shape[1]:
        df = init_df(df_comma)
    else:
        df = init_df(df_semi)
    return df
    
	
if __name__ == '__main__':
    print("main running")
    CPDataHandler('dataBase\\inf_70_20\\taskset__1643188613-a_0.7-b_0.2-n_30-m_20-d_unif-p_2000-q_4000-g_1000-t_5__0__tsk.csv')
