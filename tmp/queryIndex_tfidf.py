'''
Author: Arden Dertat
Contact: ardendertat@gmail.com
License: MIT License
'''

#!/usr/bin/env python

import sys
import re
from porterStemmer import PorterStemmer
from collections import defaultdict
import copy

porter=PorterStemmer()

class QueryIndex:

    def __init__(self):
        self.index={}
        self.titleIndex={}
        self.tf={}      #term frequencies
        self.idf={}    #inverse document frequencies


    def intersectLists(self,lists):
        if len(lists)==0:
            return []
        #start intersecting from the smaller list
        lists.sort(key=len)
        return list(reduce(lambda x,y: set(x)&set(y),lists))
        
    
    def getStopwords(self):
        f=open(self.stopwordsFile, 'r')
        stopwords=[line.rstrip() for line in f]
        self.sw=dict.fromkeys(stopwords)
        f.close()
        

    def getTerms(self, line):
        line=line.lower()
        line=re.sub(r'[^a-z0-9 ]',' ',line) #put spaces instead of non-alphanumeric characters
        line=line.split()
        line=[x for x in line if x not in self.sw]
        line=[ porter.stem(word, 0, len(word)-1) for word in line]
        return line
        
    
    def getPostings(self, terms):
        #all terms in the list are guaranteed to be in the index
        return [ self.index[term] for term in terms ]
    
    
    def getDocsFromPostings(self, postings):
        #no empty list in postings
        return [ [x[0] for x in p] for p in postings ]


    def readIndex(self):
        #read main index
        f=open(self.indexFile, 'r');
        #first read the number of documents
        self.numDocuments=int(f.readline().rstrip())
        for line in f:
            line=line.rstrip()
            term, postings, tf, idf = line.split('|')    #term='termID', postings='docID1:pos1,pos2;docID2:pos1,pos2'
            postings=postings.split(';')        #postings=['docId1:pos1,pos2','docID2:pos1,pos2']
            postings=[x.split(':') for x in postings] #postings=[['docId1', 'pos1,pos2'], ['docID2', 'pos1,pos2']]
            postings=[ [int(x[0]), map(int, x[1].split(','))] for x in postings ]   #final postings list  
            self.index[term]=postings
            #read term frequencies
            tf=tf.split(',')
            self.tf[term]=map(float, tf)
            #read inverse document frequency
            self.idf[term]=float(idf)
        f.close()
        
        #read title index
        f=open(self.titleIndexFile, 'r')
        for line in f:
            pageid, title = line.rstrip().split(' ', 1)
            self.titleIndex[int(pageid)]=title
        f.close()
        
     
    def dotProduct(self, vec1, vec2):
        if len(vec1)!=len(vec2):
            return 0
        return sum([ x*y for x,y in zip(vec1,vec2) ])
            
        
    def rankDocuments(self, terms, docs):
        #term at a time evaluation
        docVectors=defaultdict(lambda: [0]*len(terms))
        queryVector=[0]*len(terms)
        for termIndex, term in enumerate(terms):
            if term not in self.index:
                continue
            
            queryVector[termIndex]=self.idf[term]
            
            for docIndex, (doc, postings) in enumerate(self.index[term]):
                if doc in docs:
                    docVectors[doc][termIndex]=self.tf[term][docIndex]
                    
        #calculate the score of each doc
        docScores=[ [self.dotProduct(curDocVec, queryVector), doc] for doc, curDocVec in docVectors.iteritems() ]
        docScores.sort(reverse=True)
        resultDocs=[x[1] for x in docScores][:10]
        #print document titles instead if document id's
        resultDocs=[ self.titleIndex[x] for x in resultDocs ]
        print '\n'.join(resultDocs), '\n'


    def queryType(self,q):
        if '"' in q:
            return 'PQ'
        elif len(q.split()) > 1:
            return 'FTQ'
        else:
            return 'OWQ'


    def owq(self,q):
        '''One Word Query'''
        originalQuery=q
        q=self.getTerms(q)
        if len(q)==0:
            print ''
            return
        elif len(q)>1:
            self.ftq(originalQuery)
            return
        
        #q contains only 1 term 
        term=q[0]
        if term not in self.index:
            print ''
            return
        else:
            postings=self.index[term]
            docs=[x[0] for x in postings]
            self.rankDocuments(q, docs)
          

    def ftq(self,q):
        """Free Text Query"""
        q=self.getTerms(q)
        if len(q)==0:
            print ''
            return
        
        li=set()
        for term in q:
            try:
                postings=self.index[term]
                docs=[x[0] for x in postings]
                li=li|set(docs)
            except:
                #term not in index
                pass
        
        li=list(li)
        self.rankDocuments(q, li)


    def pq(self,q):
        '''Phrase Query'''
        originalQuery=q
        q=self.getTerms(q)
        if len(q)==0:
            print ''
            return
        elif len(q)==1:
            self.owq(originalQuery)
            return

        phraseDocs=self.pqDocs(q)
        self.rankDocuments(q, phraseDocs)
        
        
    def pqDocs(self, q):
        """ here q is not the query, it is the list of terms """
        phraseDocs=[]
        length=len(q)
        #first find matching docs
        for term in q:
            if term not in self.index:
                #if a term doesn't appear in the index
                #there can't be any document maching it
                return []
        
        postings=self.getPostings(q)    #all the terms in q are in the index
        docs=self.getDocsFromPostings(postings)
        #docs are the documents that contain every term in the query
        docs=self.intersectLists(docs)
        #postings are the postings list of the terms in the documents docs only
        for i in xrange(len(postings)):
            postings[i]=[x for x in postings[i] if x[0] in docs]
        
        #check whether the term ordering in the docs is like in the phrase query
        
        #subtract i from the ith terms location in the docs
        postings=copy.deepcopy(postings)    #this is important since we are going to modify the postings list
        
        for i in xrange(len(postings)):
            for j in xrange(len(postings[i])):
                postings[i][j][1]=[x-i for x in postings[i][j][1]]
        
        #intersect the locations
        result=[]
        for i in xrange(len(postings[0])):
            li=self.intersectLists( [x[i][1] for x in postings] )
            if li==[]:
                continue
            else:
                result.append(postings[0][i][0])    #append the docid to the result
        
        return result

        
    def getParams(self):
        param=sys.argv
        self.stopwordsFile=param[1]
        self.indexFile=param[2]
        self.titleIndexFile=param[3]


    def queryIndex(self):
        self.getParams()
        self.readIndex()  
        self.getStopwords() 

        while True:
            q=sys.stdin.readline()
            if q=='':
                break

            qt=self.queryType(q)
            if qt=='OWQ':
                self.owq(q)
            elif qt=='FTQ':
                self.ftq(q)
            elif qt=='PQ':
                self.pq(q)
        
        
if __name__=='__main__':
    q=QueryIndex()
    q.queryIndex()
