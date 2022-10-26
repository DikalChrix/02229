from math import gcd
import pandas as pd
import numpy as np

def main():
    df = load_data('dataBase\\test_separation\\test_separation.csv')
    task_data = add_triples(df)
    print(task_data)
    


def add_triples(df):
    lcm = 1
    for i in df['period']:
        lcm = lcm*i//gcd(lcm, i)
    task_data = []
    for ind in df.index:
        task = []
        if df['type'][ind] != "ET":
            for i in range(int(lcm/df['period'][ind])):
                task.append((i*df['deadline'][ind],df['duration'][ind],df['priority'][ind]))
            task_data.append(task)
    return task_data
    
def load_data(file):
    df = pd.read_csv(file, usecols = [i for i in range(1,7)])
    df['duration'] = df['duration'].apply(np.int32)
    df['period'] = df['period'].apply(np.int32)
    df['priority'] = df['priority'].apply(np.int32)
    df['deadline'] = df['deadline'].apply(np.int32)
    return df
    
	
if __name__ == '__main__':
    main()